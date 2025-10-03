const parseJsonPayload = (payload) => {
  if (!payload) {
    return null;
  }
  try {
    return JSON.parse(payload);
  } catch (err) {
    return null;
  }
};

export const aggregateListeners = (payload) => {
  const data = parseJsonPayload(payload);
  if (!data) {
    return [];
  }
  const rows = [];
  const collect = (input, origin) => {
    if (!Array.isArray(input)) {
      return;
    }
    input.forEach((item, index) => {
      const listener = item.listener || item;
      const name = listener?.name || item.name || `listener-${origin}-${index}`;
      const address = listener?.address?.socket_address || listener?.address?.envoy_internal_address;
      const socketAddress = listener?.address?.socket_address;
      const filterChains = Array.isArray(listener?.filter_chains)
        ? listener.filter_chains.flatMap((chain) => (
            Array.isArray(chain.filters)
              ? chain.filters.map((filter) => filter?.name).filter(Boolean)
              : []
          ))
        : [];
      rows.push({
        id: `${origin}-${index}-${name}`,
        name,
        origin,
        address: socketAddress?.address || address?.server_listener_name || '-',
        port: socketAddress?.port_value || '-',
        filters: filterChains,
      });
    });
  };

  collect(data.static_listeners, 'static');
  collect(data.dynamic_listeners, 'dynamic');
  collect(data.warming_listeners, 'warming');
  return rows;
};

export const aggregateClusters = (payload) => {
  const data = parseJsonPayload(payload);
  if (!data) {
    return [];
  }
  const rows = [];
  const collectClusters = (collection, origin, accessor = (item) => item.cluster || item) => {
    if (!Array.isArray(collection)) {
      return;
    }
    collection.forEach((item, index) => {
      const cluster = accessor(item) || {};
      const name = cluster.name || item.name || `cluster-${origin}-${index}`;
      const type = cluster.type || cluster.discovery_type || item.type || '-';
      let endpointCount = 0;
      const loadAssignment = cluster.load_assignment?.endpoints;
      if (Array.isArray(loadAssignment)) {
        endpointCount = loadAssignment.reduce((acc, ep) => (
          acc + (Array.isArray(ep.lb_endpoints) ? ep.lb_endpoints.length : 0)
        ), 0);
      }
      if (Array.isArray(item.host_statuses)) {
        endpointCount = item.host_statuses.length;
      }
      rows.push({
        id: `${origin}-${index}-${name}`,
        name,
        origin,
        type,
        endpointCount,
        addedViaApi: item.added_via_api ?? cluster.added_via_api ?? undefined,
      });
    });
  };

  collectClusters(data.dynamic_active_clusters, 'dynamic');
  collectClusters(data.dynamic_warming_clusters, 'warming');
  collectClusters(data.static_clusters, 'static');
  collectClusters(data.cluster_statuses, 'status', (item) => item);
  return rows;
};

export const aggregateRoutesFromDump = (payload) => {
  const dump = parseJsonPayload(payload);
  if (!dump) {
    return [];
  }
  const routes = [];
  const handleEntry = (entry, origin) => {
    if (!entry) {
      return;
    }
    const routeConfig = entry.route_config || entry.config || entry;
    const name = routeConfig?.name || entry.name || `route-${origin}-${routes.length}`;
    const virtualHosts = Array.isArray(routeConfig?.virtual_hosts) ? routeConfig.virtual_hosts : [];
    const routeCount = virtualHosts.reduce((acc, vh) => acc + (Array.isArray(vh.routes) ? vh.routes.length : 0), 0);
    const domains = virtualHosts.flatMap((vh) => (
      Array.isArray(vh.domains) ? vh.domains : []
    ));
    routes.push({
      id: `${origin}-${name}-${routes.length}`,
      name,
      origin,
      virtualHostCount: virtualHosts.length,
      routeCount,
      sampleDomains: domains.slice(0, 5),
      totalDomains: domains.length,
    });
  };

  const processContainer = (container, origin) => {
    if (!Array.isArray(container)) {
      return;
    }
    container.forEach((item) => handleEntry(item, origin));
  };

  if (Array.isArray(dump)) {
    dump.forEach((config) => {
      processContainer(config.static_route_configs, 'static');
      processContainer(config.dynamic_route_configs, 'dynamic');
      processContainer(config.inline_route_configs, 'inline');
    });
  } else {
    processContainer(dump.static_route_configs, 'static');
    processContainer(dump.dynamic_route_configs, 'dynamic');
    processContainer(dump.inline_route_configs, 'inline');
  }
  return routes;
};