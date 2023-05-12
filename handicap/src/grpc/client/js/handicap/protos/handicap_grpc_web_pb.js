/**
 * @fileoverview gRPC-Web generated client stub for handicap.grpc
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!


/* eslint-disable */
// @ts-nocheck



const grpc = {};
grpc.web = require('grpc-web');

const proto = {};
proto.handicap = {};
proto.handicap.grpc = require('./handicap_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?grpc.web.ClientOptions} options
 * @constructor
 * @struct
 * @final
 */
proto.handicap.grpc.HandicapIndexClient =
    function(hostname, credentials, options) {
  if (!options) options = {};
  options.format = 'text';

  /**
   * @private @const {!grpc.web.GrpcWebClientBase} The client
   */
  this.client_ = new grpc.web.GrpcWebClientBase(options);

  /**
   * @private @const {string} The hostname
   */
  this.hostname_ = hostname;

};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?grpc.web.ClientOptions} options
 * @constructor
 * @struct
 * @final
 */
proto.handicap.grpc.HandicapIndexPromiseClient =
    function(hostname, credentials, options) {
  if (!options) options = {};
  options.format = 'text';

  /**
   * @private @const {!grpc.web.GrpcWebClientBase} The client
   */
  this.client_ = new grpc.web.GrpcWebClientBase(options);

  /**
   * @private @const {string} The hostname
   */
  this.hostname_ = hostname;

};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.handicap.grpc.Command,
 *   !proto.handicap.grpc.ListCoursesResponse>}
 */
const methodDescriptor_HandicapIndex_ListCourses = new grpc.web.MethodDescriptor(
  '/handicap.grpc.HandicapIndex/ListCourses',
  grpc.web.MethodType.UNARY,
  proto.handicap.grpc.Command,
  proto.handicap.grpc.ListCoursesResponse,
  /**
   * @param {!proto.handicap.grpc.Command} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.handicap.grpc.ListCoursesResponse.deserializeBinary
);


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.RpcError, ?proto.handicap.grpc.ListCoursesResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.handicap.grpc.ListCoursesResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.handicap.grpc.HandicapIndexClient.prototype.listCourses =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/ListCourses',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_ListCourses,
      callback);
};


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>=} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.handicap.grpc.ListCoursesResponse>}
 *     Promise that resolves to the response
 */
proto.handicap.grpc.HandicapIndexPromiseClient.prototype.listCourses =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/ListCourses',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_ListCourses);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.handicap.grpc.Command,
 *   !proto.handicap.grpc.HandicapData>}
 */
const methodDescriptor_HandicapIndex_AddRating = new grpc.web.MethodDescriptor(
  '/handicap.grpc.HandicapIndex/AddRating',
  grpc.web.MethodType.UNARY,
  proto.handicap.grpc.Command,
  proto.handicap.grpc.HandicapData,
  /**
   * @param {!proto.handicap.grpc.Command} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.handicap.grpc.HandicapData.deserializeBinary
);


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.RpcError, ?proto.handicap.grpc.HandicapData)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.handicap.grpc.HandicapData>|undefined}
 *     The XHR Node Readable Stream
 */
proto.handicap.grpc.HandicapIndexClient.prototype.addRating =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/AddRating',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_AddRating,
      callback);
};


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>=} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.handicap.grpc.HandicapData>}
 *     Promise that resolves to the response
 */
proto.handicap.grpc.HandicapIndexPromiseClient.prototype.addRating =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/AddRating',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_AddRating);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.handicap.grpc.Command,
 *   !proto.handicap.grpc.HandicapData>}
 */
const methodDescriptor_HandicapIndex_AddScore = new grpc.web.MethodDescriptor(
  '/handicap.grpc.HandicapIndex/AddScore',
  grpc.web.MethodType.UNARY,
  proto.handicap.grpc.Command,
  proto.handicap.grpc.HandicapData,
  /**
   * @param {!proto.handicap.grpc.Command} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.handicap.grpc.HandicapData.deserializeBinary
);


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.RpcError, ?proto.handicap.grpc.HandicapData)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.handicap.grpc.HandicapData>|undefined}
 *     The XHR Node Readable Stream
 */
proto.handicap.grpc.HandicapIndexClient.prototype.addScore =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/AddScore',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_AddScore,
      callback);
};


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>=} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.handicap.grpc.HandicapData>}
 *     Promise that resolves to the response
 */
proto.handicap.grpc.HandicapIndexPromiseClient.prototype.addScore =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/AddScore',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_AddScore);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.handicap.grpc.Command,
 *   !proto.handicap.grpc.HandicapData>}
 */
const methodDescriptor_HandicapIndex_RemoveScore = new grpc.web.MethodDescriptor(
  '/handicap.grpc.HandicapIndex/RemoveScore',
  grpc.web.MethodType.UNARY,
  proto.handicap.grpc.Command,
  proto.handicap.grpc.HandicapData,
  /**
   * @param {!proto.handicap.grpc.Command} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.handicap.grpc.HandicapData.deserializeBinary
);


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.RpcError, ?proto.handicap.grpc.HandicapData)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.handicap.grpc.HandicapData>|undefined}
 *     The XHR Node Readable Stream
 */
proto.handicap.grpc.HandicapIndexClient.prototype.removeScore =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/RemoveScore',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_RemoveScore,
      callback);
};


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>=} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.handicap.grpc.HandicapData>}
 *     Promise that resolves to the response
 */
proto.handicap.grpc.HandicapIndexPromiseClient.prototype.removeScore =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/RemoveScore',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_RemoveScore);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.handicap.grpc.Command,
 *   !proto.handicap.grpc.HandicapData>}
 */
const methodDescriptor_HandicapIndex_GolferScores = new grpc.web.MethodDescriptor(
  '/handicap.grpc.HandicapIndex/GolferScores',
  grpc.web.MethodType.UNARY,
  proto.handicap.grpc.Command,
  proto.handicap.grpc.HandicapData,
  /**
   * @param {!proto.handicap.grpc.Command} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.handicap.grpc.HandicapData.deserializeBinary
);


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.RpcError, ?proto.handicap.grpc.HandicapData)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.handicap.grpc.HandicapData>|undefined}
 *     The XHR Node Readable Stream
 */
proto.handicap.grpc.HandicapIndexClient.prototype.golferScores =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/GolferScores',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_GolferScores,
      callback);
};


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>=} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.handicap.grpc.HandicapData>}
 *     Promise that resolves to the response
 */
proto.handicap.grpc.HandicapIndexPromiseClient.prototype.golferScores =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/GolferScores',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_GolferScores);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.handicap.grpc.HandicapSetup,
 *   !proto.handicap.grpc.HandicapData>}
 */
const methodDescriptor_HandicapIndex_GetGolfer = new grpc.web.MethodDescriptor(
  '/handicap.grpc.HandicapIndex/GetGolfer',
  grpc.web.MethodType.UNARY,
  proto.handicap.grpc.HandicapSetup,
  proto.handicap.grpc.HandicapData,
  /**
   * @param {!proto.handicap.grpc.HandicapSetup} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.handicap.grpc.HandicapData.deserializeBinary
);


/**
 * @param {!proto.handicap.grpc.HandicapSetup} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.RpcError, ?proto.handicap.grpc.HandicapData)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.handicap.grpc.HandicapData>|undefined}
 *     The XHR Node Readable Stream
 */
proto.handicap.grpc.HandicapIndexClient.prototype.getGolfer =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/GetGolfer',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_GetGolfer,
      callback);
};


/**
 * @param {!proto.handicap.grpc.HandicapSetup} request The
 *     request proto
 * @param {?Object<string, string>=} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.handicap.grpc.HandicapData>}
 *     Promise that resolves to the response
 */
proto.handicap.grpc.HandicapIndexPromiseClient.prototype.getGolfer =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/GetGolfer',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_GetGolfer);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.handicap.grpc.Command,
 *   !proto.handicap.grpc.ListPublicGolfers>}
 */
const methodDescriptor_HandicapIndex_ListGolfers = new grpc.web.MethodDescriptor(
  '/handicap.grpc.HandicapIndex/ListGolfers',
  grpc.web.MethodType.UNARY,
  proto.handicap.grpc.Command,
  proto.handicap.grpc.ListPublicGolfers,
  /**
   * @param {!proto.handicap.grpc.Command} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.handicap.grpc.ListPublicGolfers.deserializeBinary
);


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.RpcError, ?proto.handicap.grpc.ListPublicGolfers)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.handicap.grpc.ListPublicGolfers>|undefined}
 *     The XHR Node Readable Stream
 */
proto.handicap.grpc.HandicapIndexClient.prototype.listGolfers =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/ListGolfers',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_ListGolfers,
      callback);
};


/**
 * @param {!proto.handicap.grpc.Command} request The
 *     request proto
 * @param {?Object<string, string>=} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.handicap.grpc.ListPublicGolfers>}
 *     Promise that resolves to the response
 */
proto.handicap.grpc.HandicapIndexPromiseClient.prototype.listGolfers =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/handicap.grpc.HandicapIndex/ListGolfers',
      request,
      metadata || {},
      methodDescriptor_HandicapIndex_ListGolfers);
};


module.exports = proto.handicap.grpc;

