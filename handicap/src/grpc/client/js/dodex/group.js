
export { parseGroup, getSendData }

let sendData = {};

const parseGroup = (content) => {
	let split = content.split(" ");
	let split2 = split;
	const groupData = [];
	const finalContent = [];
	const parsedData = {};

	for (let i = 0; i < split.length; i++) {
		const entry = split[i];
		const findCommand = /@group|[+|-|=|:].*/ // looking for a group command
		const found = findCommand.test(entry);
		if (found) {
			groupData.push(entry);
			const isTrue = /([+|-|=|:])$/.test(entry)
			if (entry.length === 1 || isTrue) {
				groupData.push(split[i + 1]); // add in name/member to group data
				split = split.filter(item => item !== split[i + 1]); // remove from group data - don't process again
			}
			split2 = split2.filter(item => item !== entry); // remove group data from possible message data
		} else {
			finalContent.push(entry); // Anything not involved with command use as message.
		}
	}
	parsedData.groupData = groupData.join("");
	parsedData.finalContent = finalContent.join(" ");
	return parsedData;
}

const setSendData = (parsedData, sendData, extract) => {
    let groupName = /^(.*?)$/.exec(parsedData.groupData.substring(extract.index + 1));

    if (groupName) {
        try {
            groupName = /^(.*?)[:|<br>]/.exec(parsedData.groupData.substring(extract.index + 1)).pop();
        } catch (e) {
            groupName = /^(.*?)$/.exec(parsedData.groupData.substring(extract.index + 1)).pop();
        }
    }

    let memberName = /:(.*?)$/.exec(parsedData.groupData.substring(extract.index + 1));
    if (memberName) {
        memberName = /:(.*?)$/.exec(parsedData.groupData.substring(extract.index + 1)).pop();
    }

    if (/@group|[+]|-|=|:.*/.test(groupName + memberName) || (!groupName || !groupName.length || groupName.trim() === "<br>")) {
        try {
            throw new Error("Invalid Group Command " + content)
        } catch (e) {
            console.error(e);
            sendData.status = -1;
            sendData.errorMessage = e.message;
            return sendData;
        }
    }

    sendData.groupName = groupName;
    sendData.memberName = memberName;
    sendData.groupMessage = parsedData.finalContent;
    sendData.status = 0;
    sendData.errorMessage = null;
    sendData.groupOwner = document.querySelector(".handle").innerHTML;
    sendData.ownerId = doDexMess.socket.url.split("id=")[1];
    return sendData;
}

const getSendData = (parsedData, content) => {
	let uri;

	if (parsedData.groupData && parsedData.groupData.startsWith("@group")) {
		const codes = ["-", "[+]", "="];
		let command;
		let memberName;
		sendData = {};

		for (let i = 0; i < 3; i++) {
			const extract = new RegExp(codes[i]).exec(parsedData.groupData);

			if (extract) {
				command = extract[0];
			} else {
				continue;
			}

			switch (command) {
				case "+": uri = "/addGroup";
                    sendData = setSendData(parsedData, sendData, extract);
                    sendData.method = "PUT";
					sendData.uri = uri;
					break;
				case "-": uri = "/removeGroup";
				    sendData = setSendData(parsedData, sendData, extract);
				    sendData.method = "DELETE";
                    sendData.uri = uri;
					break;
				case "=":
                    sendData = setSendData(parsedData, sendData, extract);
                    sendData.method = "POST";
                    sendData.uri = "/getGroup/" + sendData.groupName;
					break;
				default:
					console.log("Error Command/groupName******:", command, sendData.groupName)
					break;
			}
		}

		if (!command && !sendData.groupName) {
			try {
				throw new Error("Invalid Group Command " + content);
			} catch (e) {
				console.error(e);
				sendData.status = -1;
				sendData.errorMessage = e.message;
				return sendData;
			}
		}
		return sendData;
	}
}
