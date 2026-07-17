const isLocal = ['localhost', '127.0.0.1', ''].includes(globalThis.location?.hostname ?? '');

export const environment = {
  apiUrl: isLocal ? 'http://localhost:5000/api' : 'https://roadrescue-backend.onrender.com/api',
  socketUrl: isLocal ? 'http://localhost:5000' : 'https://roadrescue-backend.onrender.com'
};
