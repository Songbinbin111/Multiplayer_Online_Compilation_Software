import ReactDOM from 'react-dom/client';
import App from './App';


const __origWarn = console.warn;
const __origError = console.error;
console.warn = (...args: any[]) => {
  const m = args[0];
  if (typeof m === 'string' && m.includes('findDOMNode is deprecated')) return;
  __origWarn(...args);
};
console.error = (...args: any[]) => {
  const m = args[0];
  if (typeof m === 'string' && m.includes('findDOMNode is deprecated')) return;
  __origError(...args);
};

ReactDOM.createRoot(document.getElementById('app')!).render(
  <App />
);
