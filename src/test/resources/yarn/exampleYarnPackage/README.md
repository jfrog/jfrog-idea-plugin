This is an example package that can be used to test Yarn.

It has the common default fields in its `package.json`, along with production and development dependencies as well that
are specific to the package we have created.

## The `package.json`

### Default Package Fields

`yarn init` produces a default `package.json` similar to:

```
{
  "name": "example-yarn-package",
  "version": "1.0.0",
  "description": "An example package to demonstrate Yarn",
  "main": "index.js",
  "repository": {
    "url": "github.com/yarnpkg/example-yarn-package",
    "type": "git"
  },
  "author": "Yarn Contributors",
  "license": "BSD-2-Clause",
}
```

### Custom Package Fields

You can add custom fields to your `package.json` as well. In our case, we have added 4 custom fields.

The `scripts` field are for any special scripts that you want to use when running `yarn`. For example, here we add a
script called `test` that calls the Jest test runner because we added Jest tests to our Yarn package.

```
"scripts": {
  "test": "jest"
},
```

The `dependencies` field lists the other packages that this package is dependent upon. Our example package is dependent
on [Lodash](https://lodash.com/) since we use its `multiply` function.

```
"dependencies": {
  "lodash": "^4.16.2"
},
```

The `devDependencies` field lists the other packages that this package is dependent upon *during development*. Our
example package is dependent on [Jest](https://facebook.github.io/jest/) since we created some Jest-enabled tests for
our package.

```
"devDependencies": {
  "jest-cli": "15.1.1"
},
```

The `jest` field is a custom field specific to the Jest package we included as a dev dependency. In this case, we are
testing
in a node environment at the command-line.

```
"jest": {
  "testEnvironment": "node"
}
```

> It is important to note that Lodash and Jest are not required for Yarn. They are just examples of what can be used
> when you are creating the code for your Yarn package.

## Development

```
$ yarn install
$ yarn run test
```

## Production

```
$ yarn install --production
```
