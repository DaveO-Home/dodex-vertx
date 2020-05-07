// Per Tutorial: Let’s Build a Redux Powered React Application - Robin Orheden

import { createStore } from "redux";

let store = null;

function createState () {
    let defaultState = {
        tools: {
            items: []
        }
    };

    return defaultState;
}

function findEntry (message, items) {
    let idx = -1;
    let current = -1;
    items.forEach((item, index) => {
        if (item.message === message) {
            idx = index;
            item.displayed = true;
        } else if (item.displayed === true) {
            current = index;
            item.displayed = false;
        }
    });

    return { "idx": idx, "current": current };
}

function toolsApp (state, action) {
    let newState = null;
    let found = -1;

    switch (action.type) {
        case "ADD_TOOLS":
            newState = Object.assign({}, state);
            found = findEntry(action.message, newState.tools.items);
            if (found.idx === -1) {
                newState.tools.items.push({
                    message: action.message,
                    displayed: true,
                    index: newState.tools.items.length,
                    current: newState.tools.items.length
                });
            }

            return newState;

        case "REPLACE_TOOLS":
            newState = Object.assign({}, state);
            found = findEntry(newState.tools.items[action.index].message, newState.tools.items);
            return newState;
/*
        case 'DELETE_TOOLS':
            let items = [].concat(state.tools.items)

            items.splice(action.index, 1)

            return Object.assign({}, state, {
                tools: {
                    items: items
                }
            })
*/
        case "CLEAR_TOOLS":
            return Object.assign({}, state, {
                tools: {
                    items: []
                }
            });

        default:
            return state;
    }
}

function addTools (message) {
    return {
        type: "ADD_TOOLS",
        message: message,
        displayed: true
    };
}

function replaceTools (index) {
    return {
        type: "REPLACE_TOOLS",
        index: index,
        displayed: true
    };
}

// function deleteTools (index) {
//     return {
//         type: 'DELETE_TOOLS',
//         index: index
//     }
// }

// function clearTools () {
//     return {
//         type: 'CLEAR_TOOLS'
//     }
// }

function buildStateManagement () {
    store = createStore(toolsApp, createState());
}

export default {
    toolsStateManagement () {
        buildStateManagement();
    },
    getStore () {
        return store;
    },
    addCategory (message) {
        return store.dispatch(addTools(message));
    },
    replaceCategory (index) {
        return store.dispatch(replaceTools(index));
    },
    /* eslint no-unused-vars: ["error", { "args": "none" }] */
    findEntry (message, items) {
        return findEntry(message, store.getState().tools.items);
    }
};
