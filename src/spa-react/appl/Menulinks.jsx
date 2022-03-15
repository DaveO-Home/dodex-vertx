import React from "react";
import { Routes, Route, Link, HashRouter, Outlet } from "react-router-dom";
import ReactDOM from "react-dom";
import Start from "./components/StartC";
import Pdf from "./components/PdfC";
import Tools from "./components/ToolsC";
import Contact from "./components/ContactC";
import Welcome from "./components/HelloWorldC";
import Login from "./components/LoginC";
import Dodex from "./components/DodexC";

const SideBar = () => (
    <div className="collapse small show" id="submenu1" aria-expanded="true">
        <ul className="flex-column nav pl-4">
            <li className="nav-item">
                <Link to="/"><i className="fa fa-fw fa-home"></i> Home</Link>
            </li>
            <li className="nav-item" >
                <Link to={{ pathname: "/pdf/test" }}><i className="far fa-fw fa-file-pdf"></i> PDF View</Link>
            </li>
            <li className="nav-header nav-item">Statistics</li>
            <li className="nav-item">
                <Link to="/table/tools"><i className="fa fa-fw fa-table"></i> Tabular View</Link>
            </li>
            <li className="nav-header nav-item">React</li>
            <li className="nav-item">
                <Link to="/welcome"><i className="far fa-fw fa-hand-paper"></i> React Welcome</Link>
            </li>
        </ul>
        <div className="content">
            <Routes>
                <Route exact path="/" element={<Start />} />
                <Route path="/pdf/test" element={<Pdf />} />
                <Route path="/table/tools" element={<Tools />} />
                <Route path="/contact" element={<Contact />} />
                <Route path="/welcome" element={<Welcome />} />
                <Route path="!" element={<Start />} />
                <Route element={<Start />} />
            </Routes>
        </div>
    </div>
);

const Menulinks = () => (
    <HashRouter>
        <SideBar />
        <Outlet />
    </HashRouter>
);

const Dodexlink = () => (
    <Dodex />
);

if (window.__karma__ === undefined || (typeof window.testit !== "undefined" && !window.testit)) {
    if (document.getElementById("nav-login")) {
        ReactDOM.render(
            <Login />,
            document.getElementById("nav-login")
        );
    }
}

export { Menulinks };
export { Dodexlink };