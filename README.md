# holi-android
Android app that implements the HOLI net protocol, currently on version 0.1.

## Protocol Definition
HOLI protocol messages consist of a header and a body. The header specifies information about the client and the version of the protocol used.

### Header Specification
HOLI protocol headers consist of a title and 2 fields that must always contain valid values. Each one of these components must be in a line of its own (that is, there must be a `\n` after each).

Using regex notation, an HOLI header is as follows:

```
HOLI Protocol Version [0-9]+\.[0-9]+
Client-Token: [a-zA-Z0-9_\-]{162}
Client-Address: (?:[0-9]{1,3}\.){3}[0-9]{1,3}
```

### Body Specification
The body of an HOLI protocol message consists of 4 fields that follow the same format as the header fields.

```
Latitude: 
Longitude: 
Speed: 
Altitude: 
```


