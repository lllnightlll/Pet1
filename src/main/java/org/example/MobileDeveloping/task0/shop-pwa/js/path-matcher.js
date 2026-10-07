class PathMatcher {
  constructor(routeList) {
    this.routeList = routeList;
  }

  match(path) {
    for (const route of this.routeList) {
      const matched = path.match(route.pattern);
      if (matched) {
        return { route, params: { id: matched[1] } };
      }
    }
    return null;
  }
}
