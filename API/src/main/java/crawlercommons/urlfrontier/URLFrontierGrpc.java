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
  private static volatile io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.GetParams,
      crawlercommons.urlfrontier.Urlfrontier.StringList> getListQueuesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListQueues",
      requestType = crawlercommons.urlfrontier.Urlfrontier.GetParams.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.GetParams,
      crawlercommons.urlfrontier.Urlfrontier.StringList> getListQueuesMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.GetParams, crawlercommons.urlfrontier.Urlfrontier.StringList> getListQueuesMethod;
    if ((getListQueuesMethod = URLFrontierGrpc.getListQueuesMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getListQueuesMethod = URLFrontierGrpc.getListQueuesMethod) == null) {
          URLFrontierGrpc.getListQueuesMethod = getListQueuesMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.GetParams, crawlercommons.urlfrontier.Urlfrontier.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListQueues"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.GetParams.getDefaultInstance()))
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
      crawlercommons.urlfrontier.Urlfrontier.URLItem> getGetURLsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetURLs",
      requestType = crawlercommons.urlfrontier.Urlfrontier.GetParams.class,
      responseType = crawlercommons.urlfrontier.Urlfrontier.URLItem.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.GetParams,
      crawlercommons.urlfrontier.Urlfrontier.URLItem> getGetURLsMethod() {
    io.grpc.MethodDescriptor<crawlercommons.urlfrontier.Urlfrontier.GetParams, crawlercommons.urlfrontier.Urlfrontier.URLItem> getGetURLsMethod;
    if ((getGetURLsMethod = URLFrontierGrpc.getGetURLsMethod) == null) {
      synchronized (URLFrontierGrpc.class) {
        if ((getGetURLsMethod = URLFrontierGrpc.getGetURLsMethod) == null) {
          URLFrontierGrpc.getGetURLsMethod = getGetURLsMethod =
              io.grpc.MethodDescriptor.<crawlercommons.urlfrontier.Urlfrontier.GetParams, crawlercommons.urlfrontier.Urlfrontier.URLItem>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetURLs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.GetParams.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  crawlercommons.urlfrontier.Urlfrontier.URLItem.getDefaultInstance()))
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
     ** Returns the names of up to N active queues
     *a queue is active if it has URLs due for fetching
     * </pre>
     */
    public void listQueues(crawlercommons.urlfrontier.Urlfrontier.GetParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getListQueuesMethod(), responseObserver);
    }

    /**
     * <pre>
     ** streams URLs due for fetching from M queues with up to N items per queue *
     * </pre>
     */
    public void getURLs(crawlercommons.urlfrontier.Urlfrontier.GetParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> responseObserver) {
      asyncUnimplementedUnaryCall(getGetURLsMethod(), responseObserver);
    }

    /**
     * <pre>
     ** update / create a batch of URLs *
     * </pre>
     */
    public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> putURLs(
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver) {
      return asyncUnimplementedStreamingCall(getPutURLsMethod(), responseObserver);
    }

    /**
     * <pre>
     ** returns stats for a specific queue or the whole crawl if the value if empty or null *
     * </pre>
     */
    public void getStats(crawlercommons.urlfrontier.Urlfrontier.String request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats> responseObserver) {
      asyncUnimplementedUnaryCall(getGetStatsMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getListQueuesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.GetParams,
                crawlercommons.urlfrontier.Urlfrontier.StringList>(
                  this, METHODID_LIST_QUEUES)))
          .addMethod(
            getGetURLsMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                crawlercommons.urlfrontier.Urlfrontier.GetParams,
                crawlercommons.urlfrontier.Urlfrontier.URLItem>(
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
     ** Returns the names of up to N active queues
     *a queue is active if it has URLs due for fetching
     * </pre>
     */
    public void listQueues(crawlercommons.urlfrontier.Urlfrontier.GetParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListQueuesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** streams URLs due for fetching from M queues with up to N items per queue *
     * </pre>
     */
    public void getURLs(crawlercommons.urlfrontier.Urlfrontier.GetParams request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGetURLsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     ** update / create a batch of URLs *
     * </pre>
     */
    public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> putURLs(
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getPutURLsMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     ** returns stats for a specific queue or the whole crawl if the value if empty or null *
     * </pre>
     */
    public void getStats(crawlercommons.urlfrontier.Urlfrontier.String request,
        io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetStatsMethod(), getCallOptions()), request, responseObserver);
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
     ** Returns the names of up to N active queues
     *a queue is active if it has URLs due for fetching
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.StringList listQueues(crawlercommons.urlfrontier.Urlfrontier.GetParams request) {
      return blockingUnaryCall(
          getChannel(), getListQueuesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** streams URLs due for fetching from M queues with up to N items per queue *
     * </pre>
     */
    public java.util.Iterator<crawlercommons.urlfrontier.Urlfrontier.URLItem> getURLs(
        crawlercommons.urlfrontier.Urlfrontier.GetParams request) {
      return blockingServerStreamingCall(
          getChannel(), getGetURLsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     ** returns stats for a specific queue or the whole crawl if the value if empty or null *
     * </pre>
     */
    public crawlercommons.urlfrontier.Urlfrontier.Stats getStats(crawlercommons.urlfrontier.Urlfrontier.String request) {
      return blockingUnaryCall(
          getChannel(), getGetStatsMethod(), getCallOptions(), request);
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
     ** Returns the names of up to N active queues
     *a queue is active if it has URLs due for fetching
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.StringList> listQueues(
        crawlercommons.urlfrontier.Urlfrontier.GetParams request) {
      return futureUnaryCall(
          getChannel().newCall(getListQueuesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     ** returns stats for a specific queue or the whole crawl if the value if empty or null *
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<crawlercommons.urlfrontier.Urlfrontier.Stats> getStats(
        crawlercommons.urlfrontier.Urlfrontier.String request) {
      return futureUnaryCall(
          getChannel().newCall(getGetStatsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_QUEUES = 0;
  private static final int METHODID_GET_URLS = 1;
  private static final int METHODID_GET_STATS = 2;
  private static final int METHODID_PUT_URLS = 3;

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
          serviceImpl.listQueues((crawlercommons.urlfrontier.Urlfrontier.GetParams) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>) responseObserver);
          break;
        case METHODID_GET_URLS:
          serviceImpl.getURLs((crawlercommons.urlfrontier.Urlfrontier.GetParams) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem>) responseObserver);
          break;
        case METHODID_GET_STATS:
          serviceImpl.getStats((crawlercommons.urlfrontier.Urlfrontier.String) request,
              (io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats>) responseObserver);
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
              .build();
        }
      }
    }
    return result;
  }
}
