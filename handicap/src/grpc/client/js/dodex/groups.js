
import { parseGroup, getSendData } from "./group.js";

/*
    This is a dodex-mess override to add in a group capability. It uses 'openapi' and communicates
    via 'rest' calls to the server. See groupApi31.yml for details.
*/
export { groupListener };

const debug = false;

const groupListener = () => {
    let dials;
    try {
        dials = document.querySelectorAll(".dial");
        for (const dial of dials) {
            if (!window.observer) {
                dial.addEventListener("dblclick", observe);
            }
        }
    } catch (e) {
        console.error("Error: " + e.message);
    }
}

const sendMessage = async () => {
    let userMes = document.querySelector("#dodexComm").querySelector(".user-msg");
    const selectUser = document.querySelector("#dodexComm").querySelector("select");
    var content = userMes.innerHTML.replace(/&nbsp;/g, '').trim();

    const groupCommand = content.toLowerCase().includes("@group");
    if (content.length < 2 || content === "<br>") {
        return
    } else if (content.length > 1 && !groupCommand) {  // Use normal dodex-mess messaging
        const message = attachSelectedUsers(content);
        window.doDexMess.socket.send(message);
        userMes.innerHTML = "";
    } else { // override dodex-mess
        const parsedData = parseGroup(content);
        const sendData = getSendData(parsedData, content);

        if(!sendData) {
            chatbox.innerHTML += "Invalid input: " + content + "<br>";
            userMes.innerHTML = "";
            return;
        }
        if (sendData.status === -1) {
            chatbox.innerHTML += sendData.errorMessage + "<br>";
            userMes.innerHTML = "";
            return;
        }

        const attachedUsers = attachSelectedUsers(sendData.groupMessage);
        sendData.groupMessage = attachedUsers;
        let text = "Deleting 'group/members(s)'. Are you sure?";
          if (sendData.method === "DELETE" &&
                (sendData.groupMessage.length === 0 || sendData.groupMessage === ";users!![]") &&
                confirm(text) === false) {
            return;
          }
          if(debug) {
            console.log("Post Message:", protocol + "//" + server + sendData.uri, sendData)
          }

        const response = await groupData(protocol + "//" + server + sendData.uri, sendData, sendData.method);
        userMes.innerHTML = "";
        const value = await response.json();

        chatbox.innerHTML += typeof value === "number" ? "Failed with " + value : `${response.statusText.toLowerCase()}<br>`;
        chatbox.innerHTML += value.errorMessage !== null ? value.errorMessage + "<br>": "";
        if(debug) {
            console.log("Return data", value);
        }
        if(sendData.groupMessage !== "" && value.status === 0) {
            if(sendData.groupMessage.trim().indexOf(";users!!") > 5) {
                userMes.innerHTML = sendData.groupMessage;
                sendMessage();
            }
        }
        if(typeof value.members !== "undefined") {
            for(let index = selectUser.length - 1; index > -1; index--) {
                selectUser.removeChild(selectUser[index]);
            }
            for(let index = 0; index < value.members.length; index++) {
                const option = document.createElement("OPTION");
                option.text = value.members[index].name;
                option.value = value.members[index].name;
                selectUser.add(option);
            }
        }
    }
}

const attachSelectedUsers = (message) => {
    if(message.indexOf(";users!!") === -1) {
        const dodexComm = document.querySelector("#dodexComm");
        const selected = dodexComm.querySelector("select");

        if (dodexComm && selected) {
            const selectedUsers = getSelectedOptions(selected);
            if (dodexComm.querySelector("a[name=\"private\"]").innerHTML === "Broadcast") {
                message += ";users!!" + JSON.stringify(selectedUsers);
            }
        }
    }

    return message;
}

const getSelectedOptions = (selected) => {
    const options = [];
    let option;

    var length = selected.options.length;
    for (let i = 0; i < length; i++) {
        option = selected.options[i];

        if (option.selected) {
            options.push(option.value);
        }
    }
    return options;
}

const observe = () => {
    const targetNode = document.querySelector("#usermsg");
    if (!targetNode || window.observer) {
        return;
    }
    const config = { childList: true, subtree: true, attributes: false };

    /* Finding the smallest piece of html to observe */
    const callback = (mutationList, observer) => {
        for (const mutation of mutationList) {
            if (mutation && mutation.target && mutation.target.id === "usermsg") {
                // observer.disconnect();
                document.querySelector(".input-send").onclick = sendMessage;
                window.doDexMess.socket.onopen = () => {
                    document.querySelector(".input-send").onclick = sendMessage;
                };
                return;
            }
        }
    };

    if (!window.observer) {
        window.observer = new MutationObserver(callback);
        window.observer.observe(targetNode, config);
    } else {
        // window.observer.observe();
    }
}

const groupData = async (url = "", data = {}, method = "GET") => {
    // Default options are marked with *
    const response = await fetch(url, {
        method: method, // *GET, POST, PUT, DELETE, etc.
        mode: "cors", // no-cors, *cors, same-origin
        cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
        credentials: "same-origin", // include, *same-origin, omit
        headers: {
            "Content-Type": "application/json",
            // 'Content-Type': 'application/x-www-form-urlencoded',
        },
        redirect: "follow", // manual, *follow, error
        referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
        body: "GET" === method ? null : JSON.stringify(data), // body data type must match "Content-Type" header
    }).catch(err => {
        console.error(err.message);
    });

    return response;
}

const protocol = window.location.protocol;
const port = window.location.port == "8890" ? "8880" : window.location.port == "8888" ? "8087" : window.location.port.toString();
const server = window.location.hostname + (port.length > 0 ? ":" + port : "");
