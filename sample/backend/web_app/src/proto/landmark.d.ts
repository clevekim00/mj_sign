import * as $protobuf from "protobufjs";
import Long = require("long");
/** Namespace mj. */
export namespace mj {

    /** Namespace sign. */
    namespace sign {

        /** Properties of a Point3D. */
        interface IPoint3D {

            /** Point3D x */
            x?: (number|null);

            /** Point3D y */
            y?: (number|null);

            /** Point3D z */
            z?: (number|null);
        }

        /** Represents a Point3D. */
        class Point3D implements IPoint3D {

            /**
             * Constructs a new Point3D.
             * @param [properties] Properties to set
             */
            constructor(properties?: mj.sign.IPoint3D);

            /** Point3D x. */
            public x: number;

            /** Point3D y. */
            public y: number;

            /** Point3D z. */
            public z: number;

            /**
             * Creates a new Point3D instance using the specified properties.
             * @param [properties] Properties to set
             * @returns Point3D instance
             */
            public static create(properties?: mj.sign.IPoint3D): mj.sign.Point3D;

            /**
             * Encodes the specified Point3D message. Does not implicitly {@link mj.sign.Point3D.verify|verify} messages.
             * @param message Point3D message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            public static encode(message: mj.sign.IPoint3D, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Encodes the specified Point3D message, length delimited. Does not implicitly {@link mj.sign.Point3D.verify|verify} messages.
             * @param message Point3D message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            public static encodeDelimited(message: mj.sign.IPoint3D, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Decodes a Point3D message from the specified reader or buffer.
             * @param reader Reader or buffer to decode from
             * @param [length] Message length if known beforehand
             * @returns Point3D
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): mj.sign.Point3D;

            /**
             * Decodes a Point3D message from the specified reader or buffer, length delimited.
             * @param reader Reader or buffer to decode from
             * @returns Point3D
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): mj.sign.Point3D;

            /**
             * Verifies a Point3D message.
             * @param message Plain object to verify
             * @returns `null` if valid, otherwise the reason why it is not
             */
            public static verify(message: { [k: string]: any }): (string|null);

            /**
             * Creates a Point3D message from a plain object. Also converts values to their respective internal types.
             * @param object Plain object
             * @returns Point3D
             */
            public static fromObject(object: { [k: string]: any }): mj.sign.Point3D;

            /**
             * Creates a plain object from a Point3D message. Also converts values to other types if specified.
             * @param message Point3D
             * @param [options] Conversion options
             * @returns Plain object
             */
            public static toObject(message: mj.sign.Point3D, options?: $protobuf.IConversionOptions): { [k: string]: any };

            /**
             * Converts this Point3D to JSON.
             * @returns JSON object
             */
            public toJSON(): { [k: string]: any };

            /**
             * Gets the default type url for Point3D
             * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
             * @returns The default type url
             */
            public static getTypeUrl(typeUrlPrefix?: string): string;
        }

        /** Properties of a LandmarkFrame. */
        interface ILandmarkFrame {

            /** LandmarkFrame timestampMs */
            timestampMs?: (number|Long|null);

            /** LandmarkFrame leftHand */
            leftHand?: (mj.sign.IPoint3D[]|null);

            /** LandmarkFrame rightHand */
            rightHand?: (mj.sign.IPoint3D[]|null);

            /** LandmarkFrame pose */
            pose?: (mj.sign.IPoint3D[]|null);

            /** LandmarkFrame faceContour */
            faceContour?: (mj.sign.IPoint3D[]|null);
        }

        /** Represents a LandmarkFrame. */
        class LandmarkFrame implements ILandmarkFrame {

            /**
             * Constructs a new LandmarkFrame.
             * @param [properties] Properties to set
             */
            constructor(properties?: mj.sign.ILandmarkFrame);

            /** LandmarkFrame timestampMs. */
            public timestampMs: (number|Long);

            /** LandmarkFrame leftHand. */
            public leftHand: mj.sign.IPoint3D[];

            /** LandmarkFrame rightHand. */
            public rightHand: mj.sign.IPoint3D[];

            /** LandmarkFrame pose. */
            public pose: mj.sign.IPoint3D[];

            /** LandmarkFrame faceContour. */
            public faceContour: mj.sign.IPoint3D[];

            /**
             * Creates a new LandmarkFrame instance using the specified properties.
             * @param [properties] Properties to set
             * @returns LandmarkFrame instance
             */
            public static create(properties?: mj.sign.ILandmarkFrame): mj.sign.LandmarkFrame;

            /**
             * Encodes the specified LandmarkFrame message. Does not implicitly {@link mj.sign.LandmarkFrame.verify|verify} messages.
             * @param message LandmarkFrame message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            public static encode(message: mj.sign.ILandmarkFrame, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Encodes the specified LandmarkFrame message, length delimited. Does not implicitly {@link mj.sign.LandmarkFrame.verify|verify} messages.
             * @param message LandmarkFrame message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            public static encodeDelimited(message: mj.sign.ILandmarkFrame, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Decodes a LandmarkFrame message from the specified reader or buffer.
             * @param reader Reader or buffer to decode from
             * @param [length] Message length if known beforehand
             * @returns LandmarkFrame
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): mj.sign.LandmarkFrame;

            /**
             * Decodes a LandmarkFrame message from the specified reader or buffer, length delimited.
             * @param reader Reader or buffer to decode from
             * @returns LandmarkFrame
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): mj.sign.LandmarkFrame;

            /**
             * Verifies a LandmarkFrame message.
             * @param message Plain object to verify
             * @returns `null` if valid, otherwise the reason why it is not
             */
            public static verify(message: { [k: string]: any }): (string|null);

            /**
             * Creates a LandmarkFrame message from a plain object. Also converts values to their respective internal types.
             * @param object Plain object
             * @returns LandmarkFrame
             */
            public static fromObject(object: { [k: string]: any }): mj.sign.LandmarkFrame;

            /**
             * Creates a plain object from a LandmarkFrame message. Also converts values to other types if specified.
             * @param message LandmarkFrame
             * @param [options] Conversion options
             * @returns Plain object
             */
            public static toObject(message: mj.sign.LandmarkFrame, options?: $protobuf.IConversionOptions): { [k: string]: any };

            /**
             * Converts this LandmarkFrame to JSON.
             * @returns JSON object
             */
            public toJSON(): { [k: string]: any };

            /**
             * Gets the default type url for LandmarkFrame
             * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
             * @returns The default type url
             */
            public static getTypeUrl(typeUrlPrefix?: string): string;
        }

        /** Properties of a ClientStreamChunk. */
        interface IClientStreamChunk {

            /** ClientStreamChunk sessionId */
            sessionId?: (string|null);

            /** ClientStreamChunk frames */
            frames?: (mj.sign.ILandmarkFrame[]|null);
        }

        /** Represents a ClientStreamChunk. */
        class ClientStreamChunk implements IClientStreamChunk {

            /**
             * Constructs a new ClientStreamChunk.
             * @param [properties] Properties to set
             */
            constructor(properties?: mj.sign.IClientStreamChunk);

            /** ClientStreamChunk sessionId. */
            public sessionId: string;

            /** ClientStreamChunk frames. */
            public frames: mj.sign.ILandmarkFrame[];

            /**
             * Creates a new ClientStreamChunk instance using the specified properties.
             * @param [properties] Properties to set
             * @returns ClientStreamChunk instance
             */
            public static create(properties?: mj.sign.IClientStreamChunk): mj.sign.ClientStreamChunk;

            /**
             * Encodes the specified ClientStreamChunk message. Does not implicitly {@link mj.sign.ClientStreamChunk.verify|verify} messages.
             * @param message ClientStreamChunk message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            public static encode(message: mj.sign.IClientStreamChunk, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Encodes the specified ClientStreamChunk message, length delimited. Does not implicitly {@link mj.sign.ClientStreamChunk.verify|verify} messages.
             * @param message ClientStreamChunk message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            public static encodeDelimited(message: mj.sign.IClientStreamChunk, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Decodes a ClientStreamChunk message from the specified reader or buffer.
             * @param reader Reader or buffer to decode from
             * @param [length] Message length if known beforehand
             * @returns ClientStreamChunk
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): mj.sign.ClientStreamChunk;

            /**
             * Decodes a ClientStreamChunk message from the specified reader or buffer, length delimited.
             * @param reader Reader or buffer to decode from
             * @returns ClientStreamChunk
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): mj.sign.ClientStreamChunk;

            /**
             * Verifies a ClientStreamChunk message.
             * @param message Plain object to verify
             * @returns `null` if valid, otherwise the reason why it is not
             */
            public static verify(message: { [k: string]: any }): (string|null);

            /**
             * Creates a ClientStreamChunk message from a plain object. Also converts values to their respective internal types.
             * @param object Plain object
             * @returns ClientStreamChunk
             */
            public static fromObject(object: { [k: string]: any }): mj.sign.ClientStreamChunk;

            /**
             * Creates a plain object from a ClientStreamChunk message. Also converts values to other types if specified.
             * @param message ClientStreamChunk
             * @param [options] Conversion options
             * @returns Plain object
             */
            public static toObject(message: mj.sign.ClientStreamChunk, options?: $protobuf.IConversionOptions): { [k: string]: any };

            /**
             * Converts this ClientStreamChunk to JSON.
             * @returns JSON object
             */
            public toJSON(): { [k: string]: any };

            /**
             * Gets the default type url for ClientStreamChunk
             * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
             * @returns The default type url
             */
            public static getTypeUrl(typeUrlPrefix?: string): string;
        }

        /** Properties of a TranslationResult. */
        interface ITranslationResult {

            /** TranslationResult sessionId */
            sessionId?: (string|null);

            /** TranslationResult text */
            text?: (string|null);

            /** TranslationResult isFinal */
            isFinal?: (boolean|null);

            /** TranslationResult confidence */
            confidence?: (number|null);
        }

        /** Represents a TranslationResult. */
        class TranslationResult implements ITranslationResult {

            /**
             * Constructs a new TranslationResult.
             * @param [properties] Properties to set
             */
            constructor(properties?: mj.sign.ITranslationResult);

            /** TranslationResult sessionId. */
            public sessionId: string;

            /** TranslationResult text. */
            public text: string;

            /** TranslationResult isFinal. */
            public isFinal: boolean;

            /** TranslationResult confidence. */
            public confidence: number;

            /**
             * Creates a new TranslationResult instance using the specified properties.
             * @param [properties] Properties to set
             * @returns TranslationResult instance
             */
            public static create(properties?: mj.sign.ITranslationResult): mj.sign.TranslationResult;

            /**
             * Encodes the specified TranslationResult message. Does not implicitly {@link mj.sign.TranslationResult.verify|verify} messages.
             * @param message TranslationResult message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            public static encode(message: mj.sign.ITranslationResult, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Encodes the specified TranslationResult message, length delimited. Does not implicitly {@link mj.sign.TranslationResult.verify|verify} messages.
             * @param message TranslationResult message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            public static encodeDelimited(message: mj.sign.ITranslationResult, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Decodes a TranslationResult message from the specified reader or buffer.
             * @param reader Reader or buffer to decode from
             * @param [length] Message length if known beforehand
             * @returns TranslationResult
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): mj.sign.TranslationResult;

            /**
             * Decodes a TranslationResult message from the specified reader or buffer, length delimited.
             * @param reader Reader or buffer to decode from
             * @returns TranslationResult
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): mj.sign.TranslationResult;

            /**
             * Verifies a TranslationResult message.
             * @param message Plain object to verify
             * @returns `null` if valid, otherwise the reason why it is not
             */
            public static verify(message: { [k: string]: any }): (string|null);

            /**
             * Creates a TranslationResult message from a plain object. Also converts values to their respective internal types.
             * @param object Plain object
             * @returns TranslationResult
             */
            public static fromObject(object: { [k: string]: any }): mj.sign.TranslationResult;

            /**
             * Creates a plain object from a TranslationResult message. Also converts values to other types if specified.
             * @param message TranslationResult
             * @param [options] Conversion options
             * @returns Plain object
             */
            public static toObject(message: mj.sign.TranslationResult, options?: $protobuf.IConversionOptions): { [k: string]: any };

            /**
             * Converts this TranslationResult to JSON.
             * @returns JSON object
             */
            public toJSON(): { [k: string]: any };

            /**
             * Gets the default type url for TranslationResult
             * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
             * @returns The default type url
             */
            public static getTypeUrl(typeUrlPrefix?: string): string;
        }
    }
}
