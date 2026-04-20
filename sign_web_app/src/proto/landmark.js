/*eslint-disable block-scoped-var, id-length, no-control-regex, no-magic-numbers, no-prototype-builtins, no-redeclare, no-shadow, no-var, sort-vars*/
import * as $protobuf from "protobufjs/minimal";

// Common aliases
const $Reader = $protobuf.Reader, $Writer = $protobuf.Writer, $util = $protobuf.util;

// Exported root namespace
const $root = $protobuf.roots["default"] || ($protobuf.roots["default"] = {});

export const mj = $root.mj = (() => {

    /**
     * Namespace mj.
     * @exports mj
     * @namespace
     */
    const mj = {};

    mj.sign = (function() {

        /**
         * Namespace sign.
         * @memberof mj
         * @namespace
         */
        const sign = {};

        sign.Point3D = (function() {

            /**
             * Properties of a Point3D.
             * @memberof mj.sign
             * @interface IPoint3D
             * @property {number|null} [x] Point3D x
             * @property {number|null} [y] Point3D y
             * @property {number|null} [z] Point3D z
             */

            /**
             * Constructs a new Point3D.
             * @memberof mj.sign
             * @classdesc Represents a Point3D.
             * @implements IPoint3D
             * @constructor
             * @param {mj.sign.IPoint3D=} [properties] Properties to set
             */
            function Point3D(properties) {
                if (properties)
                    for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                        if (properties[keys[i]] != null)
                            this[keys[i]] = properties[keys[i]];
            }

            /**
             * Point3D x.
             * @member {number} x
             * @memberof mj.sign.Point3D
             * @instance
             */
            Point3D.prototype.x = 0;

            /**
             * Point3D y.
             * @member {number} y
             * @memberof mj.sign.Point3D
             * @instance
             */
            Point3D.prototype.y = 0;

            /**
             * Point3D z.
             * @member {number} z
             * @memberof mj.sign.Point3D
             * @instance
             */
            Point3D.prototype.z = 0;

            /**
             * Creates a new Point3D instance using the specified properties.
             * @function create
             * @memberof mj.sign.Point3D
             * @static
             * @param {mj.sign.IPoint3D=} [properties] Properties to set
             * @returns {mj.sign.Point3D} Point3D instance
             */
            Point3D.create = function create(properties) {
                return new Point3D(properties);
            };

            /**
             * Encodes the specified Point3D message. Does not implicitly {@link mj.sign.Point3D.verify|verify} messages.
             * @function encode
             * @memberof mj.sign.Point3D
             * @static
             * @param {mj.sign.IPoint3D} message Point3D message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            Point3D.encode = function encode(message, writer) {
                if (!writer)
                    writer = $Writer.create();
                if (message.x != null && Object.hasOwnProperty.call(message, "x"))
                    writer.uint32(/* id 1, wireType 5 =*/13).float(message.x);
                if (message.y != null && Object.hasOwnProperty.call(message, "y"))
                    writer.uint32(/* id 2, wireType 5 =*/21).float(message.y);
                if (message.z != null && Object.hasOwnProperty.call(message, "z"))
                    writer.uint32(/* id 3, wireType 5 =*/29).float(message.z);
                return writer;
            };

            /**
             * Encodes the specified Point3D message, length delimited. Does not implicitly {@link mj.sign.Point3D.verify|verify} messages.
             * @function encodeDelimited
             * @memberof mj.sign.Point3D
             * @static
             * @param {mj.sign.IPoint3D} message Point3D message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            Point3D.encodeDelimited = function encodeDelimited(message, writer) {
                return this.encode(message, writer).ldelim();
            };

            /**
             * Decodes a Point3D message from the specified reader or buffer.
             * @function decode
             * @memberof mj.sign.Point3D
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @param {number} [length] Message length if known beforehand
             * @returns {mj.sign.Point3D} Point3D
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            Point3D.decode = function decode(reader, length, error) {
                if (!(reader instanceof $Reader))
                    reader = $Reader.create(reader);
                let end = length === undefined ? reader.len : reader.pos + length, message = new $root.mj.sign.Point3D();
                while (reader.pos < end) {
                    let tag = reader.uint32();
                    if (tag === error)
                        break;
                    switch (tag >>> 3) {
                    case 1: {
                            message.x = reader.float();
                            break;
                        }
                    case 2: {
                            message.y = reader.float();
                            break;
                        }
                    case 3: {
                            message.z = reader.float();
                            break;
                        }
                    default:
                        reader.skipType(tag & 7);
                        break;
                    }
                }
                return message;
            };

            /**
             * Decodes a Point3D message from the specified reader or buffer, length delimited.
             * @function decodeDelimited
             * @memberof mj.sign.Point3D
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @returns {mj.sign.Point3D} Point3D
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            Point3D.decodeDelimited = function decodeDelimited(reader) {
                if (!(reader instanceof $Reader))
                    reader = new $Reader(reader);
                return this.decode(reader, reader.uint32());
            };

            /**
             * Verifies a Point3D message.
             * @function verify
             * @memberof mj.sign.Point3D
             * @static
             * @param {Object.<string,*>} message Plain object to verify
             * @returns {string|null} `null` if valid, otherwise the reason why it is not
             */
            Point3D.verify = function verify(message) {
                if (typeof message !== "object" || message === null)
                    return "object expected";
                if (message.x != null && message.hasOwnProperty("x"))
                    if (typeof message.x !== "number")
                        return "x: number expected";
                if (message.y != null && message.hasOwnProperty("y"))
                    if (typeof message.y !== "number")
                        return "y: number expected";
                if (message.z != null && message.hasOwnProperty("z"))
                    if (typeof message.z !== "number")
                        return "z: number expected";
                return null;
            };

            /**
             * Creates a Point3D message from a plain object. Also converts values to their respective internal types.
             * @function fromObject
             * @memberof mj.sign.Point3D
             * @static
             * @param {Object.<string,*>} object Plain object
             * @returns {mj.sign.Point3D} Point3D
             */
            Point3D.fromObject = function fromObject(object) {
                if (object instanceof $root.mj.sign.Point3D)
                    return object;
                let message = new $root.mj.sign.Point3D();
                if (object.x != null)
                    message.x = Number(object.x);
                if (object.y != null)
                    message.y = Number(object.y);
                if (object.z != null)
                    message.z = Number(object.z);
                return message;
            };

            /**
             * Creates a plain object from a Point3D message. Also converts values to other types if specified.
             * @function toObject
             * @memberof mj.sign.Point3D
             * @static
             * @param {mj.sign.Point3D} message Point3D
             * @param {$protobuf.IConversionOptions} [options] Conversion options
             * @returns {Object.<string,*>} Plain object
             */
            Point3D.toObject = function toObject(message, options) {
                if (!options)
                    options = {};
                let object = {};
                if (options.defaults) {
                    object.x = 0;
                    object.y = 0;
                    object.z = 0;
                }
                if (message.x != null && message.hasOwnProperty("x"))
                    object.x = options.json && !isFinite(message.x) ? String(message.x) : message.x;
                if (message.y != null && message.hasOwnProperty("y"))
                    object.y = options.json && !isFinite(message.y) ? String(message.y) : message.y;
                if (message.z != null && message.hasOwnProperty("z"))
                    object.z = options.json && !isFinite(message.z) ? String(message.z) : message.z;
                return object;
            };

            /**
             * Converts this Point3D to JSON.
             * @function toJSON
             * @memberof mj.sign.Point3D
             * @instance
             * @returns {Object.<string,*>} JSON object
             */
            Point3D.prototype.toJSON = function toJSON() {
                return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
            };

            /**
             * Gets the default type url for Point3D
             * @function getTypeUrl
             * @memberof mj.sign.Point3D
             * @static
             * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
             * @returns {string} The default type url
             */
            Point3D.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                if (typeUrlPrefix === undefined) {
                    typeUrlPrefix = "type.googleapis.com";
                }
                return typeUrlPrefix + "/mj.sign.Point3D";
            };

            return Point3D;
        })();

        sign.LandmarkFrame = (function() {

            /**
             * Properties of a LandmarkFrame.
             * @memberof mj.sign
             * @interface ILandmarkFrame
             * @property {number|Long|null} [timestampMs] LandmarkFrame timestampMs
             * @property {Array.<mj.sign.IPoint3D>|null} [leftHand] LandmarkFrame leftHand
             * @property {Array.<mj.sign.IPoint3D>|null} [rightHand] LandmarkFrame rightHand
             * @property {Array.<mj.sign.IPoint3D>|null} [pose] LandmarkFrame pose
             * @property {Array.<mj.sign.IPoint3D>|null} [faceContour] LandmarkFrame faceContour
             */

            /**
             * Constructs a new LandmarkFrame.
             * @memberof mj.sign
             * @classdesc Represents a LandmarkFrame.
             * @implements ILandmarkFrame
             * @constructor
             * @param {mj.sign.ILandmarkFrame=} [properties] Properties to set
             */
            function LandmarkFrame(properties) {
                this.leftHand = [];
                this.rightHand = [];
                this.pose = [];
                this.faceContour = [];
                if (properties)
                    for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                        if (properties[keys[i]] != null)
                            this[keys[i]] = properties[keys[i]];
            }

            /**
             * LandmarkFrame timestampMs.
             * @member {number|Long} timestampMs
             * @memberof mj.sign.LandmarkFrame
             * @instance
             */
            LandmarkFrame.prototype.timestampMs = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

            /**
             * LandmarkFrame leftHand.
             * @member {Array.<mj.sign.IPoint3D>} leftHand
             * @memberof mj.sign.LandmarkFrame
             * @instance
             */
            LandmarkFrame.prototype.leftHand = $util.emptyArray;

            /**
             * LandmarkFrame rightHand.
             * @member {Array.<mj.sign.IPoint3D>} rightHand
             * @memberof mj.sign.LandmarkFrame
             * @instance
             */
            LandmarkFrame.prototype.rightHand = $util.emptyArray;

            /**
             * LandmarkFrame pose.
             * @member {Array.<mj.sign.IPoint3D>} pose
             * @memberof mj.sign.LandmarkFrame
             * @instance
             */
            LandmarkFrame.prototype.pose = $util.emptyArray;

            /**
             * LandmarkFrame faceContour.
             * @member {Array.<mj.sign.IPoint3D>} faceContour
             * @memberof mj.sign.LandmarkFrame
             * @instance
             */
            LandmarkFrame.prototype.faceContour = $util.emptyArray;

            /**
             * Creates a new LandmarkFrame instance using the specified properties.
             * @function create
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {mj.sign.ILandmarkFrame=} [properties] Properties to set
             * @returns {mj.sign.LandmarkFrame} LandmarkFrame instance
             */
            LandmarkFrame.create = function create(properties) {
                return new LandmarkFrame(properties);
            };

            /**
             * Encodes the specified LandmarkFrame message. Does not implicitly {@link mj.sign.LandmarkFrame.verify|verify} messages.
             * @function encode
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {mj.sign.ILandmarkFrame} message LandmarkFrame message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            LandmarkFrame.encode = function encode(message, writer) {
                if (!writer)
                    writer = $Writer.create();
                if (message.timestampMs != null && Object.hasOwnProperty.call(message, "timestampMs"))
                    writer.uint32(/* id 1, wireType 0 =*/8).int64(message.timestampMs);
                if (message.leftHand != null && message.leftHand.length)
                    for (let i = 0; i < message.leftHand.length; ++i)
                        $root.mj.sign.Point3D.encode(message.leftHand[i], writer.uint32(/* id 2, wireType 2 =*/18).fork()).ldelim();
                if (message.rightHand != null && message.rightHand.length)
                    for (let i = 0; i < message.rightHand.length; ++i)
                        $root.mj.sign.Point3D.encode(message.rightHand[i], writer.uint32(/* id 3, wireType 2 =*/26).fork()).ldelim();
                if (message.pose != null && message.pose.length)
                    for (let i = 0; i < message.pose.length; ++i)
                        $root.mj.sign.Point3D.encode(message.pose[i], writer.uint32(/* id 4, wireType 2 =*/34).fork()).ldelim();
                if (message.faceContour != null && message.faceContour.length)
                    for (let i = 0; i < message.faceContour.length; ++i)
                        $root.mj.sign.Point3D.encode(message.faceContour[i], writer.uint32(/* id 5, wireType 2 =*/42).fork()).ldelim();
                return writer;
            };

            /**
             * Encodes the specified LandmarkFrame message, length delimited. Does not implicitly {@link mj.sign.LandmarkFrame.verify|verify} messages.
             * @function encodeDelimited
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {mj.sign.ILandmarkFrame} message LandmarkFrame message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            LandmarkFrame.encodeDelimited = function encodeDelimited(message, writer) {
                return this.encode(message, writer).ldelim();
            };

            /**
             * Decodes a LandmarkFrame message from the specified reader or buffer.
             * @function decode
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @param {number} [length] Message length if known beforehand
             * @returns {mj.sign.LandmarkFrame} LandmarkFrame
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            LandmarkFrame.decode = function decode(reader, length, error) {
                if (!(reader instanceof $Reader))
                    reader = $Reader.create(reader);
                let end = length === undefined ? reader.len : reader.pos + length, message = new $root.mj.sign.LandmarkFrame();
                while (reader.pos < end) {
                    let tag = reader.uint32();
                    if (tag === error)
                        break;
                    switch (tag >>> 3) {
                    case 1: {
                            message.timestampMs = reader.int64();
                            break;
                        }
                    case 2: {
                            if (!(message.leftHand && message.leftHand.length))
                                message.leftHand = [];
                            message.leftHand.push($root.mj.sign.Point3D.decode(reader, reader.uint32()));
                            break;
                        }
                    case 3: {
                            if (!(message.rightHand && message.rightHand.length))
                                message.rightHand = [];
                            message.rightHand.push($root.mj.sign.Point3D.decode(reader, reader.uint32()));
                            break;
                        }
                    case 4: {
                            if (!(message.pose && message.pose.length))
                                message.pose = [];
                            message.pose.push($root.mj.sign.Point3D.decode(reader, reader.uint32()));
                            break;
                        }
                    case 5: {
                            if (!(message.faceContour && message.faceContour.length))
                                message.faceContour = [];
                            message.faceContour.push($root.mj.sign.Point3D.decode(reader, reader.uint32()));
                            break;
                        }
                    default:
                        reader.skipType(tag & 7);
                        break;
                    }
                }
                return message;
            };

            /**
             * Decodes a LandmarkFrame message from the specified reader or buffer, length delimited.
             * @function decodeDelimited
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @returns {mj.sign.LandmarkFrame} LandmarkFrame
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            LandmarkFrame.decodeDelimited = function decodeDelimited(reader) {
                if (!(reader instanceof $Reader))
                    reader = new $Reader(reader);
                return this.decode(reader, reader.uint32());
            };

            /**
             * Verifies a LandmarkFrame message.
             * @function verify
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {Object.<string,*>} message Plain object to verify
             * @returns {string|null} `null` if valid, otherwise the reason why it is not
             */
            LandmarkFrame.verify = function verify(message) {
                if (typeof message !== "object" || message === null)
                    return "object expected";
                if (message.timestampMs != null && message.hasOwnProperty("timestampMs"))
                    if (!$util.isInteger(message.timestampMs) && !(message.timestampMs && $util.isInteger(message.timestampMs.low) && $util.isInteger(message.timestampMs.high)))
                        return "timestampMs: integer|Long expected";
                if (message.leftHand != null && message.hasOwnProperty("leftHand")) {
                    if (!Array.isArray(message.leftHand))
                        return "leftHand: array expected";
                    for (let i = 0; i < message.leftHand.length; ++i) {
                        let error = $root.mj.sign.Point3D.verify(message.leftHand[i]);
                        if (error)
                            return "leftHand." + error;
                    }
                }
                if (message.rightHand != null && message.hasOwnProperty("rightHand")) {
                    if (!Array.isArray(message.rightHand))
                        return "rightHand: array expected";
                    for (let i = 0; i < message.rightHand.length; ++i) {
                        let error = $root.mj.sign.Point3D.verify(message.rightHand[i]);
                        if (error)
                            return "rightHand." + error;
                    }
                }
                if (message.pose != null && message.hasOwnProperty("pose")) {
                    if (!Array.isArray(message.pose))
                        return "pose: array expected";
                    for (let i = 0; i < message.pose.length; ++i) {
                        let error = $root.mj.sign.Point3D.verify(message.pose[i]);
                        if (error)
                            return "pose." + error;
                    }
                }
                if (message.faceContour != null && message.hasOwnProperty("faceContour")) {
                    if (!Array.isArray(message.faceContour))
                        return "faceContour: array expected";
                    for (let i = 0; i < message.faceContour.length; ++i) {
                        let error = $root.mj.sign.Point3D.verify(message.faceContour[i]);
                        if (error)
                            return "faceContour." + error;
                    }
                }
                return null;
            };

            /**
             * Creates a LandmarkFrame message from a plain object. Also converts values to their respective internal types.
             * @function fromObject
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {Object.<string,*>} object Plain object
             * @returns {mj.sign.LandmarkFrame} LandmarkFrame
             */
            LandmarkFrame.fromObject = function fromObject(object) {
                if (object instanceof $root.mj.sign.LandmarkFrame)
                    return object;
                let message = new $root.mj.sign.LandmarkFrame();
                if (object.timestampMs != null)
                    if ($util.Long)
                        (message.timestampMs = $util.Long.fromValue(object.timestampMs)).unsigned = false;
                    else if (typeof object.timestampMs === "string")
                        message.timestampMs = parseInt(object.timestampMs, 10);
                    else if (typeof object.timestampMs === "number")
                        message.timestampMs = object.timestampMs;
                    else if (typeof object.timestampMs === "object")
                        message.timestampMs = new $util.LongBits(object.timestampMs.low >>> 0, object.timestampMs.high >>> 0).toNumber();
                if (object.leftHand) {
                    if (!Array.isArray(object.leftHand))
                        throw TypeError(".mj.sign.LandmarkFrame.leftHand: array expected");
                    message.leftHand = [];
                    for (let i = 0; i < object.leftHand.length; ++i) {
                        if (typeof object.leftHand[i] !== "object")
                            throw TypeError(".mj.sign.LandmarkFrame.leftHand: object expected");
                        message.leftHand[i] = $root.mj.sign.Point3D.fromObject(object.leftHand[i]);
                    }
                }
                if (object.rightHand) {
                    if (!Array.isArray(object.rightHand))
                        throw TypeError(".mj.sign.LandmarkFrame.rightHand: array expected");
                    message.rightHand = [];
                    for (let i = 0; i < object.rightHand.length; ++i) {
                        if (typeof object.rightHand[i] !== "object")
                            throw TypeError(".mj.sign.LandmarkFrame.rightHand: object expected");
                        message.rightHand[i] = $root.mj.sign.Point3D.fromObject(object.rightHand[i]);
                    }
                }
                if (object.pose) {
                    if (!Array.isArray(object.pose))
                        throw TypeError(".mj.sign.LandmarkFrame.pose: array expected");
                    message.pose = [];
                    for (let i = 0; i < object.pose.length; ++i) {
                        if (typeof object.pose[i] !== "object")
                            throw TypeError(".mj.sign.LandmarkFrame.pose: object expected");
                        message.pose[i] = $root.mj.sign.Point3D.fromObject(object.pose[i]);
                    }
                }
                if (object.faceContour) {
                    if (!Array.isArray(object.faceContour))
                        throw TypeError(".mj.sign.LandmarkFrame.faceContour: array expected");
                    message.faceContour = [];
                    for (let i = 0; i < object.faceContour.length; ++i) {
                        if (typeof object.faceContour[i] !== "object")
                            throw TypeError(".mj.sign.LandmarkFrame.faceContour: object expected");
                        message.faceContour[i] = $root.mj.sign.Point3D.fromObject(object.faceContour[i]);
                    }
                }
                return message;
            };

            /**
             * Creates a plain object from a LandmarkFrame message. Also converts values to other types if specified.
             * @function toObject
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {mj.sign.LandmarkFrame} message LandmarkFrame
             * @param {$protobuf.IConversionOptions} [options] Conversion options
             * @returns {Object.<string,*>} Plain object
             */
            LandmarkFrame.toObject = function toObject(message, options) {
                if (!options)
                    options = {};
                let object = {};
                if (options.arrays || options.defaults) {
                    object.leftHand = [];
                    object.rightHand = [];
                    object.pose = [];
                    object.faceContour = [];
                }
                if (options.defaults)
                    if ($util.Long) {
                        let long = new $util.Long(0, 0, false);
                        object.timestampMs = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                    } else
                        object.timestampMs = options.longs === String ? "0" : 0;
                if (message.timestampMs != null && message.hasOwnProperty("timestampMs"))
                    if (typeof message.timestampMs === "number")
                        object.timestampMs = options.longs === String ? String(message.timestampMs) : message.timestampMs;
                    else
                        object.timestampMs = options.longs === String ? $util.Long.prototype.toString.call(message.timestampMs) : options.longs === Number ? new $util.LongBits(message.timestampMs.low >>> 0, message.timestampMs.high >>> 0).toNumber() : message.timestampMs;
                if (message.leftHand && message.leftHand.length) {
                    object.leftHand = [];
                    for (let j = 0; j < message.leftHand.length; ++j)
                        object.leftHand[j] = $root.mj.sign.Point3D.toObject(message.leftHand[j], options);
                }
                if (message.rightHand && message.rightHand.length) {
                    object.rightHand = [];
                    for (let j = 0; j < message.rightHand.length; ++j)
                        object.rightHand[j] = $root.mj.sign.Point3D.toObject(message.rightHand[j], options);
                }
                if (message.pose && message.pose.length) {
                    object.pose = [];
                    for (let j = 0; j < message.pose.length; ++j)
                        object.pose[j] = $root.mj.sign.Point3D.toObject(message.pose[j], options);
                }
                if (message.faceContour && message.faceContour.length) {
                    object.faceContour = [];
                    for (let j = 0; j < message.faceContour.length; ++j)
                        object.faceContour[j] = $root.mj.sign.Point3D.toObject(message.faceContour[j], options);
                }
                return object;
            };

            /**
             * Converts this LandmarkFrame to JSON.
             * @function toJSON
             * @memberof mj.sign.LandmarkFrame
             * @instance
             * @returns {Object.<string,*>} JSON object
             */
            LandmarkFrame.prototype.toJSON = function toJSON() {
                return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
            };

            /**
             * Gets the default type url for LandmarkFrame
             * @function getTypeUrl
             * @memberof mj.sign.LandmarkFrame
             * @static
             * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
             * @returns {string} The default type url
             */
            LandmarkFrame.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                if (typeUrlPrefix === undefined) {
                    typeUrlPrefix = "type.googleapis.com";
                }
                return typeUrlPrefix + "/mj.sign.LandmarkFrame";
            };

            return LandmarkFrame;
        })();

        sign.ClientStreamChunk = (function() {

            /**
             * Properties of a ClientStreamChunk.
             * @memberof mj.sign
             * @interface IClientStreamChunk
             * @property {string|null} [sessionId] ClientStreamChunk sessionId
             * @property {Array.<mj.sign.ILandmarkFrame>|null} [frames] ClientStreamChunk frames
             */

            /**
             * Constructs a new ClientStreamChunk.
             * @memberof mj.sign
             * @classdesc Represents a ClientStreamChunk.
             * @implements IClientStreamChunk
             * @constructor
             * @param {mj.sign.IClientStreamChunk=} [properties] Properties to set
             */
            function ClientStreamChunk(properties) {
                this.frames = [];
                if (properties)
                    for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                        if (properties[keys[i]] != null)
                            this[keys[i]] = properties[keys[i]];
            }

            /**
             * ClientStreamChunk sessionId.
             * @member {string} sessionId
             * @memberof mj.sign.ClientStreamChunk
             * @instance
             */
            ClientStreamChunk.prototype.sessionId = "";

            /**
             * ClientStreamChunk frames.
             * @member {Array.<mj.sign.ILandmarkFrame>} frames
             * @memberof mj.sign.ClientStreamChunk
             * @instance
             */
            ClientStreamChunk.prototype.frames = $util.emptyArray;

            /**
             * Creates a new ClientStreamChunk instance using the specified properties.
             * @function create
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {mj.sign.IClientStreamChunk=} [properties] Properties to set
             * @returns {mj.sign.ClientStreamChunk} ClientStreamChunk instance
             */
            ClientStreamChunk.create = function create(properties) {
                return new ClientStreamChunk(properties);
            };

            /**
             * Encodes the specified ClientStreamChunk message. Does not implicitly {@link mj.sign.ClientStreamChunk.verify|verify} messages.
             * @function encode
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {mj.sign.IClientStreamChunk} message ClientStreamChunk message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            ClientStreamChunk.encode = function encode(message, writer) {
                if (!writer)
                    writer = $Writer.create();
                if (message.sessionId != null && Object.hasOwnProperty.call(message, "sessionId"))
                    writer.uint32(/* id 1, wireType 2 =*/10).string(message.sessionId);
                if (message.frames != null && message.frames.length)
                    for (let i = 0; i < message.frames.length; ++i)
                        $root.mj.sign.LandmarkFrame.encode(message.frames[i], writer.uint32(/* id 2, wireType 2 =*/18).fork()).ldelim();
                return writer;
            };

            /**
             * Encodes the specified ClientStreamChunk message, length delimited. Does not implicitly {@link mj.sign.ClientStreamChunk.verify|verify} messages.
             * @function encodeDelimited
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {mj.sign.IClientStreamChunk} message ClientStreamChunk message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            ClientStreamChunk.encodeDelimited = function encodeDelimited(message, writer) {
                return this.encode(message, writer).ldelim();
            };

            /**
             * Decodes a ClientStreamChunk message from the specified reader or buffer.
             * @function decode
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @param {number} [length] Message length if known beforehand
             * @returns {mj.sign.ClientStreamChunk} ClientStreamChunk
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            ClientStreamChunk.decode = function decode(reader, length, error) {
                if (!(reader instanceof $Reader))
                    reader = $Reader.create(reader);
                let end = length === undefined ? reader.len : reader.pos + length, message = new $root.mj.sign.ClientStreamChunk();
                while (reader.pos < end) {
                    let tag = reader.uint32();
                    if (tag === error)
                        break;
                    switch (tag >>> 3) {
                    case 1: {
                            message.sessionId = reader.string();
                            break;
                        }
                    case 2: {
                            if (!(message.frames && message.frames.length))
                                message.frames = [];
                            message.frames.push($root.mj.sign.LandmarkFrame.decode(reader, reader.uint32()));
                            break;
                        }
                    default:
                        reader.skipType(tag & 7);
                        break;
                    }
                }
                return message;
            };

            /**
             * Decodes a ClientStreamChunk message from the specified reader or buffer, length delimited.
             * @function decodeDelimited
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @returns {mj.sign.ClientStreamChunk} ClientStreamChunk
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            ClientStreamChunk.decodeDelimited = function decodeDelimited(reader) {
                if (!(reader instanceof $Reader))
                    reader = new $Reader(reader);
                return this.decode(reader, reader.uint32());
            };

            /**
             * Verifies a ClientStreamChunk message.
             * @function verify
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {Object.<string,*>} message Plain object to verify
             * @returns {string|null} `null` if valid, otherwise the reason why it is not
             */
            ClientStreamChunk.verify = function verify(message) {
                if (typeof message !== "object" || message === null)
                    return "object expected";
                if (message.sessionId != null && message.hasOwnProperty("sessionId"))
                    if (!$util.isString(message.sessionId))
                        return "sessionId: string expected";
                if (message.frames != null && message.hasOwnProperty("frames")) {
                    if (!Array.isArray(message.frames))
                        return "frames: array expected";
                    for (let i = 0; i < message.frames.length; ++i) {
                        let error = $root.mj.sign.LandmarkFrame.verify(message.frames[i]);
                        if (error)
                            return "frames." + error;
                    }
                }
                return null;
            };

            /**
             * Creates a ClientStreamChunk message from a plain object. Also converts values to their respective internal types.
             * @function fromObject
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {Object.<string,*>} object Plain object
             * @returns {mj.sign.ClientStreamChunk} ClientStreamChunk
             */
            ClientStreamChunk.fromObject = function fromObject(object) {
                if (object instanceof $root.mj.sign.ClientStreamChunk)
                    return object;
                let message = new $root.mj.sign.ClientStreamChunk();
                if (object.sessionId != null)
                    message.sessionId = String(object.sessionId);
                if (object.frames) {
                    if (!Array.isArray(object.frames))
                        throw TypeError(".mj.sign.ClientStreamChunk.frames: array expected");
                    message.frames = [];
                    for (let i = 0; i < object.frames.length; ++i) {
                        if (typeof object.frames[i] !== "object")
                            throw TypeError(".mj.sign.ClientStreamChunk.frames: object expected");
                        message.frames[i] = $root.mj.sign.LandmarkFrame.fromObject(object.frames[i]);
                    }
                }
                return message;
            };

            /**
             * Creates a plain object from a ClientStreamChunk message. Also converts values to other types if specified.
             * @function toObject
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {mj.sign.ClientStreamChunk} message ClientStreamChunk
             * @param {$protobuf.IConversionOptions} [options] Conversion options
             * @returns {Object.<string,*>} Plain object
             */
            ClientStreamChunk.toObject = function toObject(message, options) {
                if (!options)
                    options = {};
                let object = {};
                if (options.arrays || options.defaults)
                    object.frames = [];
                if (options.defaults)
                    object.sessionId = "";
                if (message.sessionId != null && message.hasOwnProperty("sessionId"))
                    object.sessionId = message.sessionId;
                if (message.frames && message.frames.length) {
                    object.frames = [];
                    for (let j = 0; j < message.frames.length; ++j)
                        object.frames[j] = $root.mj.sign.LandmarkFrame.toObject(message.frames[j], options);
                }
                return object;
            };

            /**
             * Converts this ClientStreamChunk to JSON.
             * @function toJSON
             * @memberof mj.sign.ClientStreamChunk
             * @instance
             * @returns {Object.<string,*>} JSON object
             */
            ClientStreamChunk.prototype.toJSON = function toJSON() {
                return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
            };

            /**
             * Gets the default type url for ClientStreamChunk
             * @function getTypeUrl
             * @memberof mj.sign.ClientStreamChunk
             * @static
             * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
             * @returns {string} The default type url
             */
            ClientStreamChunk.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                if (typeUrlPrefix === undefined) {
                    typeUrlPrefix = "type.googleapis.com";
                }
                return typeUrlPrefix + "/mj.sign.ClientStreamChunk";
            };

            return ClientStreamChunk;
        })();

        sign.TranslationResult = (function() {

            /**
             * Properties of a TranslationResult.
             * @memberof mj.sign
             * @interface ITranslationResult
             * @property {string|null} [sessionId] TranslationResult sessionId
             * @property {string|null} [text] TranslationResult text
             * @property {boolean|null} [isFinal] TranslationResult isFinal
             * @property {number|null} [confidence] TranslationResult confidence
             */

            /**
             * Constructs a new TranslationResult.
             * @memberof mj.sign
             * @classdesc Represents a TranslationResult.
             * @implements ITranslationResult
             * @constructor
             * @param {mj.sign.ITranslationResult=} [properties] Properties to set
             */
            function TranslationResult(properties) {
                if (properties)
                    for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                        if (properties[keys[i]] != null)
                            this[keys[i]] = properties[keys[i]];
            }

            /**
             * TranslationResult sessionId.
             * @member {string} sessionId
             * @memberof mj.sign.TranslationResult
             * @instance
             */
            TranslationResult.prototype.sessionId = "";

            /**
             * TranslationResult text.
             * @member {string} text
             * @memberof mj.sign.TranslationResult
             * @instance
             */
            TranslationResult.prototype.text = "";

            /**
             * TranslationResult isFinal.
             * @member {boolean} isFinal
             * @memberof mj.sign.TranslationResult
             * @instance
             */
            TranslationResult.prototype.isFinal = false;

            /**
             * TranslationResult confidence.
             * @member {number} confidence
             * @memberof mj.sign.TranslationResult
             * @instance
             */
            TranslationResult.prototype.confidence = 0;

            /**
             * Creates a new TranslationResult instance using the specified properties.
             * @function create
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {mj.sign.ITranslationResult=} [properties] Properties to set
             * @returns {mj.sign.TranslationResult} TranslationResult instance
             */
            TranslationResult.create = function create(properties) {
                return new TranslationResult(properties);
            };

            /**
             * Encodes the specified TranslationResult message. Does not implicitly {@link mj.sign.TranslationResult.verify|verify} messages.
             * @function encode
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {mj.sign.ITranslationResult} message TranslationResult message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            TranslationResult.encode = function encode(message, writer) {
                if (!writer)
                    writer = $Writer.create();
                if (message.sessionId != null && Object.hasOwnProperty.call(message, "sessionId"))
                    writer.uint32(/* id 1, wireType 2 =*/10).string(message.sessionId);
                if (message.text != null && Object.hasOwnProperty.call(message, "text"))
                    writer.uint32(/* id 2, wireType 2 =*/18).string(message.text);
                if (message.isFinal != null && Object.hasOwnProperty.call(message, "isFinal"))
                    writer.uint32(/* id 3, wireType 0 =*/24).bool(message.isFinal);
                if (message.confidence != null && Object.hasOwnProperty.call(message, "confidence"))
                    writer.uint32(/* id 4, wireType 5 =*/37).float(message.confidence);
                return writer;
            };

            /**
             * Encodes the specified TranslationResult message, length delimited. Does not implicitly {@link mj.sign.TranslationResult.verify|verify} messages.
             * @function encodeDelimited
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {mj.sign.ITranslationResult} message TranslationResult message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            TranslationResult.encodeDelimited = function encodeDelimited(message, writer) {
                return this.encode(message, writer).ldelim();
            };

            /**
             * Decodes a TranslationResult message from the specified reader or buffer.
             * @function decode
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @param {number} [length] Message length if known beforehand
             * @returns {mj.sign.TranslationResult} TranslationResult
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            TranslationResult.decode = function decode(reader, length, error) {
                if (!(reader instanceof $Reader))
                    reader = $Reader.create(reader);
                let end = length === undefined ? reader.len : reader.pos + length, message = new $root.mj.sign.TranslationResult();
                while (reader.pos < end) {
                    let tag = reader.uint32();
                    if (tag === error)
                        break;
                    switch (tag >>> 3) {
                    case 1: {
                            message.sessionId = reader.string();
                            break;
                        }
                    case 2: {
                            message.text = reader.string();
                            break;
                        }
                    case 3: {
                            message.isFinal = reader.bool();
                            break;
                        }
                    case 4: {
                            message.confidence = reader.float();
                            break;
                        }
                    default:
                        reader.skipType(tag & 7);
                        break;
                    }
                }
                return message;
            };

            /**
             * Decodes a TranslationResult message from the specified reader or buffer, length delimited.
             * @function decodeDelimited
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @returns {mj.sign.TranslationResult} TranslationResult
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            TranslationResult.decodeDelimited = function decodeDelimited(reader) {
                if (!(reader instanceof $Reader))
                    reader = new $Reader(reader);
                return this.decode(reader, reader.uint32());
            };

            /**
             * Verifies a TranslationResult message.
             * @function verify
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {Object.<string,*>} message Plain object to verify
             * @returns {string|null} `null` if valid, otherwise the reason why it is not
             */
            TranslationResult.verify = function verify(message) {
                if (typeof message !== "object" || message === null)
                    return "object expected";
                if (message.sessionId != null && message.hasOwnProperty("sessionId"))
                    if (!$util.isString(message.sessionId))
                        return "sessionId: string expected";
                if (message.text != null && message.hasOwnProperty("text"))
                    if (!$util.isString(message.text))
                        return "text: string expected";
                if (message.isFinal != null && message.hasOwnProperty("isFinal"))
                    if (typeof message.isFinal !== "boolean")
                        return "isFinal: boolean expected";
                if (message.confidence != null && message.hasOwnProperty("confidence"))
                    if (typeof message.confidence !== "number")
                        return "confidence: number expected";
                return null;
            };

            /**
             * Creates a TranslationResult message from a plain object. Also converts values to their respective internal types.
             * @function fromObject
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {Object.<string,*>} object Plain object
             * @returns {mj.sign.TranslationResult} TranslationResult
             */
            TranslationResult.fromObject = function fromObject(object) {
                if (object instanceof $root.mj.sign.TranslationResult)
                    return object;
                let message = new $root.mj.sign.TranslationResult();
                if (object.sessionId != null)
                    message.sessionId = String(object.sessionId);
                if (object.text != null)
                    message.text = String(object.text);
                if (object.isFinal != null)
                    message.isFinal = Boolean(object.isFinal);
                if (object.confidence != null)
                    message.confidence = Number(object.confidence);
                return message;
            };

            /**
             * Creates a plain object from a TranslationResult message. Also converts values to other types if specified.
             * @function toObject
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {mj.sign.TranslationResult} message TranslationResult
             * @param {$protobuf.IConversionOptions} [options] Conversion options
             * @returns {Object.<string,*>} Plain object
             */
            TranslationResult.toObject = function toObject(message, options) {
                if (!options)
                    options = {};
                let object = {};
                if (options.defaults) {
                    object.sessionId = "";
                    object.text = "";
                    object.isFinal = false;
                    object.confidence = 0;
                }
                if (message.sessionId != null && message.hasOwnProperty("sessionId"))
                    object.sessionId = message.sessionId;
                if (message.text != null && message.hasOwnProperty("text"))
                    object.text = message.text;
                if (message.isFinal != null && message.hasOwnProperty("isFinal"))
                    object.isFinal = message.isFinal;
                if (message.confidence != null && message.hasOwnProperty("confidence"))
                    object.confidence = options.json && !isFinite(message.confidence) ? String(message.confidence) : message.confidence;
                return object;
            };

            /**
             * Converts this TranslationResult to JSON.
             * @function toJSON
             * @memberof mj.sign.TranslationResult
             * @instance
             * @returns {Object.<string,*>} JSON object
             */
            TranslationResult.prototype.toJSON = function toJSON() {
                return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
            };

            /**
             * Gets the default type url for TranslationResult
             * @function getTypeUrl
             * @memberof mj.sign.TranslationResult
             * @static
             * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
             * @returns {string} The default type url
             */
            TranslationResult.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                if (typeUrlPrefix === undefined) {
                    typeUrlPrefix = "type.googleapis.com";
                }
                return typeUrlPrefix + "/mj.sign.TranslationResult";
            };

            return TranslationResult;
        })();

        return sign;
    })();

    return mj;
})();

export { $root as default };
