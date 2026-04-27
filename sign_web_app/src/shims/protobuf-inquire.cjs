"use strict";

// Browser builds do not need protobufjs' optional Node-style require lookup.
// Returning null keeps protobufjs on its browser-safe fallbacks and avoids the
// direct eval used by @protobufjs/inquire.
module.exports = function inquire() {
  return null;
};
