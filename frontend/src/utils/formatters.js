export const formatPortList = (ports) => {
  if (!ports || ports.length === 0) {
    return 'N/A';
  }
  return ports.join(', ');
};

export const formatMatch = (match) => {
  if (!match) {
    return '';
  }
  return `${match.field}:${match.kind}=${match.value}`;
};

export const toMatchSummary = (matches) => {
  if (!Array.isArray(matches)) {
    return '';
  }
  return matches
    .map(formatMatch)
    .filter(Boolean)
    .join(', ');
};

export const buildLinkLabel = (link, nodesById) => {
  const source = nodesById?.[link.fromId];
  const destination = nodesById?.[link.toId];
  const sourceLabel = source?.name || source?.id || link.fromId || '—';
  const destinationLabel = destination?.name || destination?.id || link.toId || '—';
  const protocol = link.protocol || '—';
  const port = link.port || '—';
  const matchSummary = toMatchSummary(link.matches);

  return {
    id: `${link.fromId}-${link.toId}-${protocol}-${port}`,
    text: `${sourceLabel} -> ${protocol}:${port} -> ${destinationLabel}`,
    matchSummary,
  };
};