import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Register from './components/Register';
import DocumentList from './components/DocumentList';
import Editor from './components/Editor';
import './style.css';

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/documents" element={<DocumentList />} />
        <Route path="/editor/:docId" element={<Editor />} />
      </Routes>
    </Router>
  );
};

export default App;