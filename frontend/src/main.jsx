import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import App from "./App";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter future={{ v7_relativeSplatPath: true }}>
      <App />
      <Toaster
        position="bottom-right"
        toastOptions={{
          style: {
            background: "#0D0D0D",
            color: "#F5F0E8",
            border: "3px solid #0D0D0D",
            borderRadius: "0",
            fontFamily: '"DM Mono", monospace',
            fontSize: "13px",
            boxShadow: "4px 4px 0px #C8922A",
          },
          success: {
            iconTheme: { primary: "#1A6B3A", secondary: "#F5F0E8" },
          },
          error: {
            iconTheme: { primary: "#C0392B", secondary: "#F5F0E8" },
          },
        }}
      />
    </BrowserRouter>
  </React.StrictMode>,
);
