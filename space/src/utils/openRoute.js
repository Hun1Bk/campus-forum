export function openRouteInNewWindow(router, location) {
  const resolved = router.resolve(location);
  window.open(resolved.href, "_blank", "noopener");
}
