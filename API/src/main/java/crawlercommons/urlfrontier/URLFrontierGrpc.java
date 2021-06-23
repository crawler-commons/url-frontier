package crawlercommons.urlfrontier;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.34.0)",
    comments = "Source: urlfrontier.proto")
public final class URLFrontierGrpc {

  private URLFrontierGrpc() {}

  public static final String SERVICE_NAME = "urlfrontier.URLFrontier";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Integer,
      crawlercommons.urlfrontier.Urlfrontier.StringList> getListQueuesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListQueues",
      requestType = crawlercommons.urlfrontier.Urlfrontier.Integer.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Integer,
      crawlercommons.urlfrontier.Urlfrontier.StringList> getListQueuesMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Integer, crawlercommons.urlfrontier.Urlfrontier.StringList> getListQueuesMethod;
    if ((getListQueuesMethod = URLFrontierGrpc.getListQueuesMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getListQueuesMethod = URLFrontierGrpc.getListQueuesMethod) == null) {
          URLFrontierGrpc.getListQueuesMethod = getListQueuesMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.Integer, crawlercommons.urlfrontier.Urlfrontier.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListQueues"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Integer.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("ListQueues"))
              .build();
        }
      }
    }
    return getListQueuesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.GetParams,
      crawlercommons.urlfrontier.Urlfrontier.URLInfo> getGetURLsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetURLs",
      requestType = crawlercommons.urlfrontier.Urlfrontier.GetParams.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.URLInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.GetParams,
      crawlercommons.urlfrontier.Urlfrontier.URLInfo> getGetURLsMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.GetParams, crawlercommons.urlfrontier.Urlfrontier.URLInfo> getGetURLsMethod;
    if ((getGetURLsMethod = URLFrontierGrpc.getGetURLsMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getGetURLsMethod = URLFrontierGrpc.getGetURLsMethod) == null) {
          URLFrontierGrpc.getGetURLsMethod = getGetURLsMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.GetParams, crawlercommons.urlfrontier.Urlfrontier.URLInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetURLs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.GetParams.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.URLInfo.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("GetURLs"))
              .build();
        }
      }
    }
    return getGetURLsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.URLItem,
      crawlercommons.urlfrontier.Urlfrontier.String> getPutURLsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PutURLs",
      requestType = crawlercommons.urlfrontier.Urlfrontier.URLItem.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.String.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.URLItem,
      crawlercommons.urlfrontier.Urlfrontier.String> getPutURLsMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.URLItem, crawlercommons.urlfrontier.Urlfrontier.String> getPutURLsMethod;
    if ((getPutURLsMethod = URLFrontierGrpc.getPutURLsMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getPutURLsMethod = URLFrontierGrpc.getPutURLsMethod) == null) {
          URLFrontierGrpc.getPutURLsMethod = getPutURLsMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.URLItem, crawlercommons.urlfrontier.Urlfrontier.String>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PutURLs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.URLItem.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.String.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("PutURLs"))
              .build();
        }
      }
    }
    return getPutURLsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.String,
      crawlercommons.urlfrontier.Urlfrontier.Stats> getGetStatsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetStats",
      requestType = crawlercommons.urlfrontier.Urlfrontier.String.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.Stats.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.String,
      crawlercommons.urlfrontier.Urlfrontier.Stats> getGetStatsMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.String, crawlercommons.urlfrontier.Urlfrontier.Stats> getGetStatsMethod;
    if ((getGetStatsMethod = URLFrontierGrpc.getGetStatsMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getGetStatsMethod = URLFrontierGrpc.getGetStatsMethod) == null) {
          URLFrontierGrpc.getGetStatsMethod = getGetStatsMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.String, crawlercommons.urlfrontier.Urlfrontier.Stats>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetStats"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.String.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Stats.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("GetStats"))
              .build();
        }
      }
    }
    return getGetStatsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.String,
      crawlercommons.urlfrontier.Urlfrontier.Integer> getDeleteQueueMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteQueue",
      requestType = crawlercommons.urlfrontier.Urlfrontier.String.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.Integer.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.String,
      crawlercommons.urlfrontier.Urlfrontier.Integer> getDeleteQueueMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.String, crawlercommons.urlfrontier.Urlfrontier.Integer> getDeleteQueueMethod;
    if ((getDeleteQueueMethod = URLFrontierGrpc.getDeleteQueueMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getDeleteQueueMethod = URLFrontierGrpc.getDeleteQueueMethod) == null) {
          URLFrontierGrpc.getDeleteQueueMethod = getDeleteQueueMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.String, crawlercommons.urlfrontier.Urlfrontier.Integer>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteQueue"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.String.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Integer.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("DeleteQueue"))
              .build();
        }
      }
    }
    return getDeleteQueueMethod;
  }

  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams,
      crawlercommons.urlfrontier.Urlfrontier.Empty> getBlockQueueUntilMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BlockQueueUntil",
      requestType = crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams,
      crawlercommons.urlfrontier.Urlfrontier.Empty> getBlockQueueUntilMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams, crawlercommons.urlfrontier.Urlfrontier.Empty> getBlockQueueUntilMethod;
    if ((getBlockQueueUntilMethod = URLFrontierGrpc.getBlockQueueUntilMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getBlockQueueUntilMethod = URLFrontierGrpc.getBlockQueueUntilMethod) == null) {
          URLFrontierGrpc.getBlockQueueUntilMethod = getBlockQueueUntilMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams, crawlercommons.urlfrontier.Urlfrontier.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BlockQueueUntil"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("BlockQueueUntil"))
              .build();
        }
      }
    }
    return getBlockQueueUntilMethod;
  }

  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Boolean,
      crawlercommons.urlfrontier.Urlfrontier.Empty> getSetActiveMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetActive",
      requestType = crawlercommons.urlfrontier.Urlfrontier.Boolean.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Boolean,
      crawlercommons.urlfrontier.Urlfrontier.Empty> getSetActiveMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Boolean, crawlercommons.urlfrontier.Urlfrontier.Empty> getSetActiveMethod;
    if ((getSetActiveMethod = URLFrontierGrpc.getSetActiveMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getSetActiveMethod = URLFrontierGrpc.getSetActiveMethod) == null) {
          URLFrontierGrpc.getSetActiveMethod = getSetActiveMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.Boolean, crawlercommons.urlfrontier.Urlfrontier.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetActive"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Boolean.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("SetActive"))
              .build();
        }
      }
    }
    return getSetActiveMethod;
  }

  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Empty,
      crawlercommons.urlfrontier.Urlfrontier.Boolean> getGetActiveMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetActive",
      requestType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.Boolean.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Empty,
      crawlercommons.urlfrontier.Urlfrontier.Boolean> getGetActiveMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.Empty, crawlercommons.urlfrontier.Urlfrontier.Boolean> getGetActiveMethod;
    if ((getGetActiveMethod = URLFrontierGrpc.getGetActiveMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getGetActiveMethod = URLFrontierGrpc.getGetActiveMethod) == null) {
          URLFrontierGrpc.getGetActiveMethod = getGetActiveMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.Empty, crawlercommons.urlfrontier.Urlfrontier.Boolean>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetActive"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Boolean.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("GetActive"))
              .build();
        }
      }
    }
    return getGetActiveMethod;
  }

  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams,
      crawlercommons.urlfrontier.Urlfrontier.Empty> getSetDelayMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetDelay",
      requestType = crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams,
      crawlercommons.urlfrontier.Urlfrontier.Empty> getSetDelayMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams, crawlercommons.urlfrontier.Urlfrontier.Empty> getSetDelayMethod;
    if ((getSetDelayMethod = URLFrontierGrpc.getSetDelayMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getSetDelayMethod = URLFrontierGrpc.getSetDelayMethod) == null) {
          URLFrontierGrpc.getSetDelayMethod = getSetDelayMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams, crawlercommons.urlfrontier.Urlfrontier.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetDelay"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new URLFrontierMethodDescriptorSupplier("SetDelay"))
              .build();
        }
      }
    }
    return getSetDelayMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static URLFrontierStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<URLFrontierStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<URLFrontierStub>() {
        @java.lang.Override
        public URLFrontierStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new URLFrontierStub(channel, callOptions);
        }
      };
    return URLFrontierStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static URLFrontierBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<URLFrontierBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<URLFrontierBlockingStub>() {
        @java.lang.Override
        public URLFrontierBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new URLFrontierBlockingStub(channel, callOptions);
        }
      };
    return URLFrontierBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static URLFrontierFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<URLFrontierFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<URLFrontierFutureStub>() {
        @java.lang.Override
        public URLFrontierFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new URLFrontierFutureStub(channel, callOptions);
        }
      };
    return URLFrontierFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class URLFrontierImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     ** Return the names of up to N active queues
     *a queue is active if it has URLs due for fetching; the default value of 0 sets no limit to the number of queue names to return *
     * </pre>
     */
    public void listQueues(crawlercommons.urlfrontier.Urlfrontier.Integer request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getListQueuesMethod(), responseObserver);
    }

    /**
     * <pre>
     ** Stream URLs due for fetching from M queues with up to N items per queue *
     * </pre>
     */
    public void getURLs(crawlercommons.urlfrontier.Urlfrontier.GetParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetURLsMethod(), responseObserver);
    }

    /**
     * <pre>
     ** Push URL items to the server; they get created (if they don't already exist) in case of DiscoveredURLItems or updated if KnownURLItems *
     * </pre>
     */
    public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> putURLs(
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver) {
      return asyncUnimplementedStreamingCall(getPutURLsMethod(), responseObserver);
    }

    /**
     * <pre>
     ** Return stats for a specific queue or the whole crawl if the value if empty or null *
     * </pre>
     */
    public void getStats(crawlercommons.urlfrontier.Urlfrontier.String request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats> responseObserver) {
      asyncUnimplementedUnaryCall(getGetStatsMethod(), responseObserver);
    }

    /**
     * <pre>
     ** Delete the queue based on the key in parameter, returns the number of URLs removed this way *
     * </pre>
     */
    public void deleteQueue(crawlercommons.urlfrontier.Urlfrontier.String request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteQueueMethod(), responseObserver);
    }

    /**
     * <pre>
     ** Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch
     *1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time
     *indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance. 
     * </pre>
     */
    public void blockQueueUntil(crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getBlockQueueUntilMethod(), responseObserver);
    }

    /**
     * <pre>
     ** De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. *
     * </pre>
     */
    public void setActive(crawlercommons.urlfrontier.Urlfrontier.Boolean request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSetActiveMethod(), responseObserver);
    }

    /**
     * <pre>
     ** Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) *
     * </pre>
     */
    public void getActive(crawlercommons.urlfrontier.Urlfrontier.Empty request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Boolean> responseObserver) {
      asyncUnimplementedUnaryCall(getGetActiveMethod(), responseObserver);
    }

    /**
     * <pre>
     ** Set a delay from a given queue.
     *No URLs will be obtained via GetURLs for this queue until the number of seconds specified has 
     *elapsed since the last time URLs were retrieved.
     *Usually informed by the delay setting of robots.txt.
     * </pre>
     */
    public void setDelay(crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSetDelayMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getListQueuesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.Integer,
                crawlercommons.urlfrontier.Urlfrontier.StringList>(
                  this, METHODID_LIST_QUEUES)))
          .addMethod(
            getGetURLsMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.GetParams,
                crawlercommons.urlfrontier.Urlfrontier.URLInfo>(
                  this, METHODID_GET_URLS)))
          .addMethod(
            getPutURLsMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.URLItem,
                crawlercommons.urlfrontier.Urlfrontier.String>(
                  this, METHODID_PUT_URLS)))
          .addMethod(
            getGetStatsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.String,
                crawlercommons.urlfrontier.Urlfrontier.Stats>(
                  this, METHODID_GET_STATS)))
          .addMethod(
            getDeleteQueueMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.String,
                crawlercommons.urlfrontier.Urlfrontier.Integer>(
                  this, METHODID_DELETE_QUEUE)))
          .addMethod(
            getBlockQueueUntilMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams,
                crawlercommons.urlfrontier.Urlfrontier.Empty>(
                  this, METHODID_BLOCK_QUEUE_UNTIL)))
          .addMethod(
            getSetActiveMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.Boolean,
                crawlercommons.urlfrontier.Urlfrontier.Empty>(
                  this, METHODID_SET_ACTIVE)))
          .addMethod(
            getGetActiveMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.Empty,
                crawlercommons.urlfrontier.Urlfrontier.Boolean>(
                  this, METHODID_GET_ACTIVE)))
          .addMethod(
            getSetDelayMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams,
                crawlercommons.urlfrontier.Urlfrontier.Empty>(
                  this, METHODID_SET_DELAY)))
          .build();
    }
  }

  /**
   */
  public static final class URLFrontierStub extends io.grpc.stub.AbstractAsyncStub<URLFrontierStub> {
    private URLFrontierStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected URLFrontierStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new URLFrontierStub(channel, callOptions);
    }

    /**
     * <pre>
     ** Return the names of up to N active queues
     *a queue is active if it has URLs due for fetching; the default value of 0 sets no limit to the number of queue names to return *
     * </pre>
     */
    public void listQueues(crawlercommons.urlfrontier.Urlfrontier.Integer request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListQueuesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** Stream URLs due for fetching from M queues with up to N items per queue *
     * </pre>
     */
    public void getURLs(crawlercommons.urlfrontier.Urlfrontier.GetParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLInfo> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGetURLsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** Push URL items to the server; they get created (if they don't already exist) in case of DiscoveredURLItems or updated if KnownURLItems *
     * </pre>
     */
    public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> putURLs(
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getPutURLsMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     ** Return stats for a specific queue or the whole crawl if the value if empty or null *
     * </pre>
     */
    public void getStats(crawlercommons.urlfrontier.Urlfrontier.String request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetStatsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** Delete the queue based on the key in parameter, returns the number of URLs removed this way *
     * </pre>
     */
    public void deleteQueue(crawlercommons.urlfrontier.Urlfrontier.String request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteQueueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch
     *1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time
     *indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance. 
     * </pre>
     */
    public void blockQueueUntil(crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBlockQueueUntilMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. *
     * </pre>
     */
    public void setActive(crawlercommons.urlfrontier.Urlfrontier.Boolean request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetActiveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) *
     * </pre>
     */
    public void getActive(crawlercommons.urlfrontier.Urlfrontier.Empty request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Boolean> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetActiveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** Set a delay from a given queue.
     *No URLs will be obtained via GetURLs for this queue until the number of seconds specified has 
     *elapsed since the last time URLs were retrieved.
     *Usually informed by the delay setting of robots.txt.
     * </pre>
     */
    public void setDelay(crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetDelayMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class URLFrontierBlockingStub extends io.grpc.stub.AbstractBlockingStub<URLFrontierBlockingStub> {
    private URLFrontierBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected URLFrontierBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new URLFrontierBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     ** Return the names of up to N active queues
     *a queue is active if it has URLs due for fetching; the default value of 0 sets no limit to the number of queue names to return *
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.StringList listQueues(crawlercommons.urlfrontier.Urlfrontier.Integer request) {
      return blockingUnaryCall(
          getChannel(), getListQueuesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** Stream URLs due for fetching from M queues with up to N items per queue *
     * </pre>
     */
    public java.util.Iterator<crawlercommons.urlfrontier.Urlfrontier.URLInfo> getURLs(
        crawlercommons.urlfrontier.Urlfrontier.GetParams request) {
      return blockingServerStreamingCall(
          getChannel(), getGetURLsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** Return stats for a specific queue or the whole crawl if the value if empty or null *
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.Stats getStats(crawlercommons.urlfrontier.Urlfrontier.String request) {
      return blockingUnaryCall(
          getChannel(), getGetStatsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** Delete the queue based on the key in parameter, returns the number of URLs removed this way *
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.Integer deleteQueue(crawlercommons.urlfrontier.Urlfrontier.String request) {
      return blockingUnaryCall(
          getChannel(), getDeleteQueueMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch
     *1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time
     *indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance. 
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.Empty blockQueueUntil(crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams request) {
      return blockingUnaryCall(
          getChannel(), getBlockQueueUntilMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. *
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.Empty setActive(crawlercommons.urlfrontier.Urlfrontier.Boolean request) {
      return blockingUnaryCall(
          getChannel(), getSetActiveMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) *
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.Boolean getActive(crawlercommons.urlfrontier.Urlfrontier.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetActiveMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** Set a delay from a given queue.
     *No URLs will be obtained via GetURLs for this queue until the number of seconds specified has 
     *elapsed since the last time URLs were retrieved.
     *Usually informed by the delay setting of robots.txt.
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.Empty setDelay(crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams request) {
      return blockingUnaryCall(
          getChannel(), getSetDelayMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class URLFrontierFutureStub extends io.grpc.stub.AbstractFutureStub<URLFrontierFutureStub> {
    private URLFrontierFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected URLFrontierFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new URLFrontierFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     ** Return the names of up to N active queues
     *a queue is active if it has URLs due for fetching; the default value of 0 sets no limit to the number of queue names to return *
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.StringList> listQueues(
        crawlercommons.urlfrontier.Urlfrontier.Integer request) {
      return futureUnaryCall(
          getChannel().newCall(getListQueuesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     ** Return stats for a specific queue or the whole crawl if the value if empty or null *
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.Stats> getStats(
        crawlercommons.urlfrontier.Urlfrontier.String request) {
      return futureUnaryCall(
          getChannel().newCall(getGetStatsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     ** Delete the queue based on the key in parameter, returns the number of URLs removed this way *
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.Integer> deleteQueue(
        crawlercommons.urlfrontier.Urlfrontier.String request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteQueueMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     ** Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch
     *1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time
     *indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance. 
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.Empty> blockQueueUntil(
        crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams request) {
      return futureUnaryCall(
          getChannel().newCall(getBlockQueueUntilMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     ** De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. *
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.Empty> setActive(
        crawlercommons.urlfrontier.Urlfrontier.Boolean request) {
      return futureUnaryCall(
          getChannel().newCall(getSetActiveMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     ** Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) *
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.Boolean> getActive(
        crawlercommons.urlfrontier.Urlfrontier.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetActiveMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     ** Set a delay from a given queue.
     *No URLs will be obtained via GetURLs for this queue until the number of seconds specified has 
     *elapsed since the last time URLs were retrieved.
     *Usually informed by the delay setting of robots.txt.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.Empty> setDelay(
        crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams request) {
      return futureUnaryCall(
          getChannel().newCall(getSetDelayMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_QUEUES = 0;
  private static final int METHODID_GET_URLS = 1;
  private static final int METHODID_GET_STATS = 2;
  private static final int METHODID_DELETE_QUEUE = 3;
  private static final int METHODID_BLOCK_QUEUE_UNTIL = 4;
  private static final int METHODID_SET_ACTIVE = 5;
  private static final int METHODID_GET_ACTIVE = 6;
  private static final int METHODID_SET_DELAY = 7;
  private static final int METHODID_PUT_URLS = 8;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final URLFrontierImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(URLFrontierImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LIST_QUEUES:
          serviceImpl.listQueues((crawlercommons.urlfrontier.Urlfrontier.Integer) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>) responseObserver);
          break;
        case METHODID_GET_URLS:
          serviceImpl.getURLs((crawlercommons.urlfrontier.Urlfrontier.GetParams) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLInfo>) responseObserver);
          break;
        case METHODID_GET_STATS:
          serviceImpl.getStats((crawlercommons.urlfrontier.Urlfrontier.String) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats>) responseObserver);
          break;
        case METHODID_DELETE_QUEUE:
          serviceImpl.deleteQueue((crawlercommons.urlfrontier.Urlfrontier.String) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer>) responseObserver);
          break;
        case METHODID_BLOCK_QUEUE_UNTIL:
          serviceImpl.blockQueueUntil((crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>) responseObserver);
          break;
        case METHODID_SET_ACTIVE:
          serviceImpl.setActive((crawlercommons.urlfrontier.Urlfrontier.Boolean) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>) responseObserver);
          break;
        case METHODID_GET_ACTIVE:
          serviceImpl.getActive((crawlercommons.urlfrontier.Urlfrontier.Empty) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Boolean>) responseObserver);
          break;
        case METHODID_SET_DELAY:
          serviceImpl.setDelay((crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUT_URLS:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.putURLs(
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class URLFrontierBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    URLFrontierBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return crawlercommons.urlfrontier.Urlfrontier.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("URLFrontier");
    }
  }

  private static final class URLFrontierFileDescriptorSupplier
      extends URLFrontierBaseDescriptorSupplier {
    URLFrontierFileDescriptorSupplier() {}
  }

  private static final class URLFrontierMethodDescriptorSupplier
      extends URLFrontierBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    URLFrontierMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (URLFrontierGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new URLFrontierFileDescriptorSupplier())
              .addMethod(getListQueuesMethod())
              .addMethod(getGetURLsMethod())
              .addMethod(getPutURLsMethod())
              .addMethod(getGetStatsMethod())
              .addMethod(getDeleteQueueMethod())
              .addMethod(getBlockQueueUntilMethod())
              .addMethod(getSetActiveMethod())
              .addMethod(getGetActiveMethod())
              .addMethod(getSetDelayMethod())
              .build();
        }
      }
    }
    return result;
  }
}
