module.exports = {
    "env": {
        "browser": true,
        "es6": true,
        "jasmine": true, "jquery": true, "amd": true, "node": true
    },
    "extends": ["eslint:recommended", "plugin:react/recommended"],
    "globals": {
        "Atomics": "readonly",
        "SharedArrayBuffer": "readonly"
    },
    "parserOptions": {
        "ecmaVersion": 2018,
        "sourceType": "module"
    },
    "rules": {
        "strict": 1,
        "semi": 1,
        "quotes": 1,
        'indent': 0,
	"no-unused-vars": 1,
        "no-undef": 1,
        "no-console": [2, { allow: ["warn", "error"] }],
        "no-case-declarations": 1,
        // allow async-await
        'generator-star-spacing': 'off',
        // allow debugger during development
        'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
        'spaced-comment': ['error', 'always',
            {
                'exceptions': [
                    'removeIf(production)',
                    'endRemoveIf(production)',
                    '!steal-remove-start',
                    '!steal-remove-end'
                ]
            }],
    },
    "globals": {
        "System": true,
        "testit": true,
        "testOnly": true,
        "Stache": true,
        "steal": true,
        "rmain_container": true,
        "FuseBox": true,
        "__karma__": true,
        "spyOnEvent": true,
    },
    "settings": {
        "react": {
            "createClass": "createReactClass", // Regex for Component Factory to use,
            // default to "createReactClass"
            "pragma": "React",  // Pragma to use, default to "React"
            "version": "detect", // React version. "detect" automatically picks the version you have installed.
            "flowVersion": "0.53" // Flow version
        },
        "propWrapperFunctions": [
            // The names of any function used to wrap propTypes, e.g. `forbidExtraProps`. If this isn't set, any propTypes wrapped in a function will be skipped.
            "forbidExtraProps",
            { "property": "freeze", "object": "Object" },
            { "property": "myFavoriteWrapper" }
        ],
        "linkComponents": [
            // Components used as alternatives to <a> for linking, eg. <Link to={ url } />
            "Hyperlink",
            { "name": "Link", "linkAttribute": "to" }
        ]
    }
};
