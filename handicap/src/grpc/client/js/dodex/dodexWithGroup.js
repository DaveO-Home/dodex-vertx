import $ from "jquery";
import "dodex";
import "dodex-input";
import "dodex-mess";
import "dodex/dist/dodex.min.css";
import "jsoneditor/dist/jsoneditor.min.css"
import "bootstrap/dist/css/bootstrap.css"
import jsonEditor from "jsoneditor"
import "@fortawesome/fontawesome-free/js/all.js";
import "@fortawesome/fontawesome-free/js/fontawesome.js";
import { groupListener } from "./groups";
window.JSONEditor = jsonEditor;
import "./startGroup";
window.JSONEditor = jsonEditor;
window.groupListener = groupListener;
window.$ = window.jQuery = $
