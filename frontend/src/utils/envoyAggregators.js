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

const ensureArray = (value) => (Array.isArray(value) ? value : []);

const extractFilters = (listener) => (
  Array.isArray(listener?.filter_chains)
    ? listener.filter_chains.flatMap((chain) => (
        Array.isArray(chain.filters)
          ? chain.filters.map((filter) => filter?.name).filter(Boolean)
          : []
      ))
    : []
);

const getAddressInfo = (listener) => {
  const socketAddress = listener?.address?.socket_address;
  const internalAddress = listener?.address?.envoy_internal_address;
  return {
    address:
      socketAddress?.address
      || internalAddress?.server_listener_name
      || internalAddress?.address
      || '-',
    port: socketAddress?.port_value ?? '-',
  };
};

export const aggregateListeners = (payload) => {
  const data = parseJsonPayload(payload);
  if (!data) {
    return [];
  }

  const rows = [];
  let counter = 0;

  const pushRow = (listener, origin, state, rawContainer) => {
    if (!listener) {
      return;
    }
    counter += 1;
    const name = listener.name || `listener-${origin}-${counter}`;
    const { address, port } = getAddressInfo(listener);
    const filters = extractFilters(listener);

    rows.push({
      id: `${origin}-${state}-${counter}-${name}`,
      name,
      origin,
      state,
      address,
      port,
      filters,
      raw: rawContainer ?? listener,
      listener,
    });
  };

  ensureArray(data.static_listeners).forEach((entry) => {
    if (entry?.listener) {
      pushRow(entry.listener, 'static', 'static', entry);
    } else {
      pushRow(entry, 'static', 'static', entry);
    }
  });

  const handleDynamicEntry = (entry, origin) => {
    if (!entry) {
      return;
    }
    let added = false;
    if (entry.active_state?.listener) {
      pushRow(entry.active_state.listener, origin, 'active', entry);
      added = true;
    }
    if (entry.warming_state?.listener) {
      pushRow(entry.warming_state.listener, origin, 'warming', entry);
      added = true;
    }
    if (entry.draining_state?.listener) {
      pushRow(entry.draining_state.listener, origin, 'draining', entry);
      added = true;
    }
    if (!added && entry.listener) {
      pushRow(entry.listener, origin, entry.state || 'dynamic', entry);
    } else if (!added) {
      pushRow(entry, origin, 'dynamic', entry);
    }
  };

  const hasStructuredDynamic = (
    Array.isArray(data.dynamic_active_listeners)
    || Array.isArray(data.dynamic_warming_listeners)
    || Array.isArray(data.dynamic_draining_listeners)
  );

  ensureArray(data.dynamic_active_listeners).forEach((entry) => handleDynamicEntry(entry, 'dynamic'));
  ensureArray(data.dynamic_warming_listeners).forEach((entry) => handleDynamicEntry(entry, 'dynamic'));
  ensureArray(data.dynamic_draining_listeners).forEach((entry) => handleDynamicEntry(entry, 'dynamic'));

  if (!hasStructuredDynamic) {
    ensureArray(data.dynamic_listeners).forEach((entry) => handleDynamicEntry(entry, 'dynamic'));
  }

  ensureArray(data.listener_statuses).forEach((entry) => {
    if (entry?.listener) {
      pushRow(entry.listener, 'status', entry.state || 'status', entry);
    }
  });

  return rows;
};

export const aggregateClusters = (payload) => {
  const data = parseJsonPayload(payload);
  if (!data) {
    return [];
  }

  const rows = [];
  let counter = 0;

  const pushRow = (cluster, origin, state, rawEntry) => {
    if (!cluster) {
      return;
    }
    counter += 1;
    const name = cluster.name || `cluster-${origin}-${counter}`;
    let endpointCount = 0;
    const loadAssignment = cluster.load_assignment?.endpoints;
    if (Array.isArray(loadAssignment)) {
      endpointCount = loadAssignment.reduce((acc, ep) => (
        acc + (Array.isArray(ep.lb_endpoints) ? ep.lb_endpoints.length : 0)
      ), 0);
    }
    if (Array.isArray(rawEntry?.host_statuses)) {
      endpointCount = rawEntry.host_statuses.length;
    }

    rows.push({
      id: `${origin}-${state}-${counter}-${name}`,
      name,
      origin,
      state,
      type: cluster.type || cluster.discovery_type || rawEntry?.type || '—',
      endpointCount,
      addedViaApi: rawEntry?.added_via_api ?? cluster.added_via_api,
      raw: rawEntry ?? cluster,
      cluster,
    });
  };

  ensureArray(data.static_clusters).forEach((entry) => {
    const cluster = entry?.cluster || entry;
    pushRow(cluster, 'static', 'static', entry);
  });

  ensureArray(data.dynamic_active_clusters).forEach((entry) => {
    pushRow(entry?.cluster || entry, 'dynamic', 'active', entry);
  });

  ensureArray(data.dynamic_warming_clusters).forEach((entry) => {
    pushRow(entry?.cluster || entry, 'dynamic', 'warming', entry);
  });

  ensureArray(data.dynamic_draining_clusters).forEach((entry) => {
    pushRow(entry?.cluster || entry, 'dynamic', 'draining', entry);
  });

  ensureArray(data.cluster_statuses).forEach((entry) => {
    pushRow(entry, 'status', entry?.lifecycle_state || 'status', entry);
  });

  return rows;
};

export const aggregateRoutesFromDump = (payload) => {
  const dump = parseJsonPayload(payload);
  if (!dump) {
    return [];
  }

  const routes = [];

  const pushRoute = (entry, origin, index) => {
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
      id: `${origin}-${index}-${name}`,
      name,
      origin,
      virtualHostCount: virtualHosts.length,
      routeCount,
      sampleDomains: domains.slice(0, 5),
      totalDomains: domains.length,
      raw: routeConfig,
    });
  };

  const processContainer = (container, origin) => {
    ensureArray(container).forEach((item, index) => pushRoute(item, origin, index));
  };

  if (Array.isArray(dump)) {
    dump.forEach((config, configIndex) => {
      processContainer(config.static_route_configs, `static-${configIndex}`);
      processContainer(config.dynamic_route_configs, `dynamic-${configIndex}`);
      processContainer(config.inline_route_configs, `inline-${configIndex}`);
    });
  } else {
    processContainer(dump.static_route_configs, 'static');
    processContainer(dump.dynamic_route_configs, 'dynamic');
    processContainer(dump.inline_route_configs, 'inline');
  }

  return routes;
};