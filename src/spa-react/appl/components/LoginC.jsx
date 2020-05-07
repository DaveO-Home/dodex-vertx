import React from "react";
import start from "../js/controller/start";
import { login } from "../js/login";

class Login extends React.Component {
  render() {
    return (
      <small>
        <a href="#" className="login" onClick={handleClick}>Log In</a>
      </small>
    );
  }
}

function handleClick(e) {
  if($(".login:first").html() === "Log Out") {
    login(null, true)(e);
  } else {
    start["div .login click"](e);
  }
}

export default Login;
