import { useState } from 'react'
import './App.css'
import TopologyViewer from './components/TopologyViewer';

function App() {
  const [count, setCount] = useState(0)

  return <TopologyViewer initialNamespace="default" />;
}

export default App
