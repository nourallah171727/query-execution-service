export function decodeRole(token) {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.role || payload.authorities || null;
  } catch {
    return null;
  }
}

export function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.exp ? payload.exp * 1000 < Date.now() : false;
  } catch {
    return false;
  }
}