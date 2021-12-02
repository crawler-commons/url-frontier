package crawlercommons.urlfrontier;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/** */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.39.0)",
        comments = "Source: urlfrontier.proto")
public final class URLFrontierGrpc {

    private URLFrontierGrpc() {}

    public static final String SERVICE_NAME = "urlfrontier.URLFrontier";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Empty,
                    crawlercommons.urlfrontier.Urlfrontier.StringList>
            getListCrawlsMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "ListCrawls",
            requestType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.StringList.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Empty,
                    crawlercommons.urlfrontier.Urlfrontier.StringList>
            getListCrawlsMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.Empty,
                        crawlercommons.urlfrontier.Urlfrontier.StringList>
                getListCrawlsMethod;
        if ((getListCrawlsMethod = URLFrontierGrpc.getListCrawlsMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getListCrawlsMethod = URLFrontierGrpc.getListCrawlsMethod) == null) {
                    URLFrontierGrpc.getListCrawlsMethod =
                            getListCrawlsMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.Empty,
                                                    crawlercommons.urlfrontier.Urlfrontier
                                                            .StringList>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "ListCrawls"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Empty.getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .StringList
                                                                    .getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "ListCrawls"))
                                            .build();
                }
            }
        }
        return getListCrawlsMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.CrawlID,
                    crawlercommons.urlfrontier.Urlfrontier.Integer>
            getDeleteCrawlMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "DeleteCrawl",
            requestType = crawlercommons.urlfrontier.Urlfrontier.CrawlID.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Integer.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.CrawlID,
                    crawlercommons.urlfrontier.Urlfrontier.Integer>
            getDeleteCrawlMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.CrawlID,
                        crawlercommons.urlfrontier.Urlfrontier.Integer>
                getDeleteCrawlMethod;
        if ((getDeleteCrawlMethod = URLFrontierGrpc.getDeleteCrawlMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getDeleteCrawlMethod = URLFrontierGrpc.getDeleteCrawlMethod) == null) {
                    URLFrontierGrpc.getDeleteCrawlMethod =
                            getDeleteCrawlMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.CrawlID,
                                                    crawlercommons.urlfrontier.Urlfrontier.Integer>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "DeleteCrawl"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .CrawlID.getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Integer.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "DeleteCrawl"))
                                            .build();
                }
            }
        }
        return getDeleteCrawlMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Pagination,
                    crawlercommons.urlfrontier.Urlfrontier.QueueList>
            getListQueuesMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "ListQueues",
            requestType = crawlercommons.urlfrontier.Urlfrontier.Pagination.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.QueueList.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Pagination,
                    crawlercommons.urlfrontier.Urlfrontier.QueueList>
            getListQueuesMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.Pagination,
                        crawlercommons.urlfrontier.Urlfrontier.QueueList>
                getListQueuesMethod;
        if ((getListQueuesMethod = URLFrontierGrpc.getListQueuesMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getListQueuesMethod = URLFrontierGrpc.getListQueuesMethod) == null) {
                    URLFrontierGrpc.getListQueuesMethod =
                            getListQueuesMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.Pagination,
                                                    crawlercommons.urlfrontier.Urlfrontier
                                                            .QueueList>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "ListQueues"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Pagination
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .QueueList
                                                                    .getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "ListQueues"))
                                            .build();
                }
            }
        }
        return getListQueuesMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.GetParams,
                    crawlercommons.urlfrontier.Urlfrontier.URLInfo>
            getGetURLsMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetURLs",
            requestType = crawlercommons.urlfrontier.Urlfrontier.GetParams.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.URLInfo.class,
            methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.GetParams,
                    crawlercommons.urlfrontier.Urlfrontier.URLInfo>
            getGetURLsMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.GetParams,
                        crawlercommons.urlfrontier.Urlfrontier.URLInfo>
                getGetURLsMethod;
        if ((getGetURLsMethod = URLFrontierGrpc.getGetURLsMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getGetURLsMethod = URLFrontierGrpc.getGetURLsMethod) == null) {
                    URLFrontierGrpc.getGetURLsMethod =
                            getGetURLsMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.GetParams,
                                                    crawlercommons.urlfrontier.Urlfrontier.URLInfo>
                                                    newBuilder()
                                            .setType(
                                                    io.grpc.MethodDescriptor.MethodType
                                                            .SERVER_STREAMING)
                                            .setFullMethodName(
                                                    generateFullMethodName(SERVICE_NAME, "GetURLs"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .GetParams
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .URLInfo.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "GetURLs"))
                                            .build();
                }
            }
        }
        return getGetURLsMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.URLItem,
                    crawlercommons.urlfrontier.Urlfrontier.String>
            getPutURLsMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "PutURLs",
            requestType = crawlercommons.urlfrontier.Urlfrontier.URLItem.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.String.class,
            methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.URLItem,
                    crawlercommons.urlfrontier.Urlfrontier.String>
            getPutURLsMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.URLItem,
                        crawlercommons.urlfrontier.Urlfrontier.String>
                getPutURLsMethod;
        if ((getPutURLsMethod = URLFrontierGrpc.getPutURLsMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getPutURLsMethod = URLFrontierGrpc.getPutURLsMethod) == null) {
                    URLFrontierGrpc.getPutURLsMethod =
                            getPutURLsMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.URLItem,
                                                    crawlercommons.urlfrontier.Urlfrontier.String>
                                                    newBuilder()
                                            .setType(
                                                    io.grpc.MethodDescriptor.MethodType
                                                            .BIDI_STREAMING)
                                            .setFullMethodName(
                                                    generateFullMethodName(SERVICE_NAME, "PutURLs"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .URLItem.getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .String.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "PutURLs"))
                                            .build();
                }
            }
        }
        return getPutURLsMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams,
                    crawlercommons.urlfrontier.Urlfrontier.Stats>
            getGetStatsMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetStats",
            requestType = crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Stats.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams,
                    crawlercommons.urlfrontier.Urlfrontier.Stats>
            getGetStatsMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams,
                        crawlercommons.urlfrontier.Urlfrontier.Stats>
                getGetStatsMethod;
        if ((getGetStatsMethod = URLFrontierGrpc.getGetStatsMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getGetStatsMethod = URLFrontierGrpc.getGetStatsMethod) == null) {
                    URLFrontierGrpc.getGetStatsMethod =
                            getGetStatsMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier
                                                            .QueueWithinCrawlParams,
                                                    crawlercommons.urlfrontier.Urlfrontier.Stats>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "GetStats"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .QueueWithinCrawlParams
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Stats.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "GetStats"))
                                            .build();
                }
            }
        }
        return getGetStatsMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams,
                    crawlercommons.urlfrontier.Urlfrontier.Integer>
            getDeleteQueueMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "DeleteQueue",
            requestType = crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Integer.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams,
                    crawlercommons.urlfrontier.Urlfrontier.Integer>
            getDeleteQueueMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams,
                        crawlercommons.urlfrontier.Urlfrontier.Integer>
                getDeleteQueueMethod;
        if ((getDeleteQueueMethod = URLFrontierGrpc.getDeleteQueueMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getDeleteQueueMethod = URLFrontierGrpc.getDeleteQueueMethod) == null) {
                    URLFrontierGrpc.getDeleteQueueMethod =
                            getDeleteQueueMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier
                                                            .QueueWithinCrawlParams,
                                                    crawlercommons.urlfrontier.Urlfrontier.Integer>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "DeleteQueue"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .QueueWithinCrawlParams
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Integer.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "DeleteQueue"))
                                            .build();
                }
            }
        }
        return getDeleteQueueMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getBlockQueueUntilMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "BlockQueueUntil",
            requestType = crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getBlockQueueUntilMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams,
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                getBlockQueueUntilMethod;
        if ((getBlockQueueUntilMethod = URLFrontierGrpc.getBlockQueueUntilMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getBlockQueueUntilMethod = URLFrontierGrpc.getBlockQueueUntilMethod) == null) {
                    URLFrontierGrpc.getBlockQueueUntilMethod =
                            getBlockQueueUntilMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier
                                                            .BlockQueueParams,
                                                    crawlercommons.urlfrontier.Urlfrontier.Empty>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "BlockQueueUntil"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .BlockQueueParams
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Empty.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "BlockQueueUntil"))
                                            .build();
                }
            }
        }
        return getBlockQueueUntilMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Boolean,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetActiveMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "SetActive",
            requestType = crawlercommons.urlfrontier.Urlfrontier.Boolean.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Boolean,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetActiveMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.Boolean,
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                getSetActiveMethod;
        if ((getSetActiveMethod = URLFrontierGrpc.getSetActiveMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getSetActiveMethod = URLFrontierGrpc.getSetActiveMethod) == null) {
                    URLFrontierGrpc.getSetActiveMethod =
                            getSetActiveMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.Boolean,
                                                    crawlercommons.urlfrontier.Urlfrontier.Empty>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "SetActive"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Boolean.getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Empty.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "SetActive"))
                                            .build();
                }
            }
        }
        return getSetActiveMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Empty,
                    crawlercommons.urlfrontier.Urlfrontier.Boolean>
            getGetActiveMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetActive",
            requestType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Boolean.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Empty,
                    crawlercommons.urlfrontier.Urlfrontier.Boolean>
            getGetActiveMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.Empty,
                        crawlercommons.urlfrontier.Urlfrontier.Boolean>
                getGetActiveMethod;
        if ((getGetActiveMethod = URLFrontierGrpc.getGetActiveMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getGetActiveMethod = URLFrontierGrpc.getGetActiveMethod) == null) {
                    URLFrontierGrpc.getGetActiveMethod =
                            getGetActiveMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.Empty,
                                                    crawlercommons.urlfrontier.Urlfrontier.Boolean>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "GetActive"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Empty.getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Boolean.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "GetActive"))
                                            .build();
                }
            }
        }
        return getGetActiveMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetDelayMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "SetDelay",
            requestType = crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetDelayMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams,
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                getSetDelayMethod;
        if ((getSetDelayMethod = URLFrontierGrpc.getSetDelayMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getSetDelayMethod = URLFrontierGrpc.getSetDelayMethod) == null) {
                    URLFrontierGrpc.getSetDelayMethod =
                            getSetDelayMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier
                                                            .QueueDelayParams,
                                                    crawlercommons.urlfrontier.Urlfrontier.Empty>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "SetDelay"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .QueueDelayParams
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Empty.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "SetDelay"))
                                            .build();
                }
            }
        }
        return getSetDelayMethod;
    }

    /** Creates a new async stub that supports all call types for the service */
    public static URLFrontierStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<URLFrontierStub> factory =
                new io.grpc.stub.AbstractStub.StubFactory<URLFrontierStub>() {
                    @java.lang.Override
                    public URLFrontierStub newStub(
                            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                        return new URLFrontierStub(channel, callOptions);
                    }
                };
        return URLFrontierStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the
     * service
     */
    public static URLFrontierBlockingStub newBlockingStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<URLFrontierBlockingStub> factory =
                new io.grpc.stub.AbstractStub.StubFactory<URLFrontierBlockingStub>() {
                    @java.lang.Override
                    public URLFrontierBlockingStub newStub(
                            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                        return new URLFrontierBlockingStub(channel, callOptions);
                    }
                };
        return URLFrontierBlockingStub.newStub(factory, channel);
    }

    /** Creates a new ListenableFuture-style stub that supports unary calls on the service */
    public static URLFrontierFutureStub newFutureStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<URLFrontierFutureStub> factory =
                new io.grpc.stub.AbstractStub.StubFactory<URLFrontierFutureStub>() {
                    @java.lang.Override
                    public URLFrontierFutureStub newStub(
                            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                        return new URLFrontierFutureStub(channel, callOptions);
                    }
                };
        return URLFrontierFutureStub.newStub(factory, channel);
    }

    /** */
    public abstract static class URLFrontierImplBase implements io.grpc.BindableService {

        /**
         *
         *
         * <pre>
         * * Return the list of crawls handled by the frontier *
         * </pre>
         */
        public void listCrawls(
                crawlercommons.urlfrontier.Urlfrontier.Empty request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getListCrawlsMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Delete an entire crawl, returns the number of URLs removed this way *
         * </pre>
         */
        public void deleteCrawl(
                crawlercommons.urlfrontier.Urlfrontier.CrawlID request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getDeleteCrawlMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Return a list of queues for a specific crawl. Can chose whether to include inactive queues (a queue is active if it has URLs due for fetching);
         * by default the service will return up to 100 results from offset 0 and exclude inactive queues.*
         * </pre>
         */
        public void listQueues(
                crawlercommons.urlfrontier.Urlfrontier.Pagination request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.QueueList>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getListQueuesMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Stream URLs due for fetching from M queues with up to N items per queue *
         * </pre>
         */
        public void getURLs(
                crawlercommons.urlfrontier.Urlfrontier.GetParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLInfo>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getGetURLsMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Push URL items to the server; they get created (if they don't already exist) in case of DiscoveredURLItems or updated if KnownURLItems *
         * </pre>
         */
        public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> putURLs(
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>
                        responseObserver) {
            return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(
                    getPutURLsMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Return stats for a specific queue or the whole crawl if the value if empty or null *
         * </pre>
         */
        public void getStats(
                crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getGetStatsMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Delete the queue based on the key in parameter, returns the number of URLs removed this way *
         * </pre>
         */
        public void deleteQueue(
                crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getDeleteQueueMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch
         * 1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time
         * indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance.
         * </pre>
         */
        public void blockQueueUntil(
                crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getBlockQueueUntilMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. *
         * </pre>
         */
        public void setActive(
                crawlercommons.urlfrontier.Urlfrontier.Boolean request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getSetActiveMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) *
         * </pre>
         */
        public void getActive(
                crawlercommons.urlfrontier.Urlfrontier.Empty request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Boolean>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getGetActiveMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Set a delay from a given queue.
         * No URLs will be obtained via GetURLs for this queue until the number of seconds specified has
         * elapsed since the last time URLs were retrieved.
         * Usually informed by the delay setting of robots.txt.
         * </pre>
         */
        public void setDelay(
                crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getSetDelayMethod(), responseObserver);
        }

        @java.lang.Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                    .addMethod(
                            getListCrawlsMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.Empty,
                                            crawlercommons.urlfrontier.Urlfrontier.StringList>(
                                            this, METHODID_LIST_CRAWLS)))
                    .addMethod(
                            getDeleteCrawlMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.CrawlID,
                                            crawlercommons.urlfrontier.Urlfrontier.Integer>(
                                            this, METHODID_DELETE_CRAWL)))
                    .addMethod(
                            getListQueuesMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.Pagination,
                                            crawlercommons.urlfrontier.Urlfrontier.QueueList>(
                                            this, METHODID_LIST_QUEUES)))
                    .addMethod(
                            getGetURLsMethod(),
                            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.GetParams,
                                            crawlercommons.urlfrontier.Urlfrontier.URLInfo>(
                                            this, METHODID_GET_URLS)))
                    .addMethod(
                            getPutURLsMethod(),
                            io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.URLItem,
                                            crawlercommons.urlfrontier.Urlfrontier.String>(
                                            this, METHODID_PUT_URLS)))
                    .addMethod(
                            getGetStatsMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier
                                                    .QueueWithinCrawlParams,
                                            crawlercommons.urlfrontier.Urlfrontier.Stats>(
                                            this, METHODID_GET_STATS)))
                    .addMethod(
                            getDeleteQueueMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier
                                                    .QueueWithinCrawlParams,
                                            crawlercommons.urlfrontier.Urlfrontier.Integer>(
                                            this, METHODID_DELETE_QUEUE)))
                    .addMethod(
                            getBlockQueueUntilMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams,
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>(
                                            this, METHODID_BLOCK_QUEUE_UNTIL)))
                    .addMethod(
                            getSetActiveMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.Boolean,
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>(
                                            this, METHODID_SET_ACTIVE)))
                    .addMethod(
                            getGetActiveMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.Empty,
                                            crawlercommons.urlfrontier.Urlfrontier.Boolean>(
                                            this, METHODID_GET_ACTIVE)))
                    .addMethod(
                            getSetDelayMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams,
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>(
                                            this, METHODID_SET_DELAY)))
                    .build();
        }
    }

    /** */
    public static final class URLFrontierStub
            extends io.grpc.stub.AbstractAsyncStub<URLFrontierStub> {
        private URLFrontierStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected URLFrontierStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new URLFrontierStub(channel, callOptions);
        }

        /**
         *
         *
         * <pre>
         * * Return the list of crawls handled by the frontier *
         * </pre>
         */
        public void listCrawls(
                crawlercommons.urlfrontier.Urlfrontier.Empty request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getListCrawlsMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Delete an entire crawl, returns the number of URLs removed this way *
         * </pre>
         */
        public void deleteCrawl(
                crawlercommons.urlfrontier.Urlfrontier.CrawlID request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getDeleteCrawlMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Return a list of queues for a specific crawl. Can chose whether to include inactive queues (a queue is active if it has URLs due for fetching);
         * by default the service will return up to 100 results from offset 0 and exclude inactive queues.*
         * </pre>
         */
        public void listQueues(
                crawlercommons.urlfrontier.Urlfrontier.Pagination request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.QueueList>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getListQueuesMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Stream URLs due for fetching from M queues with up to N items per queue *
         * </pre>
         */
        public void getURLs(
                crawlercommons.urlfrontier.Urlfrontier.GetParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLInfo>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncServerStreamingCall(
                    getChannel().newCall(getGetURLsMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Push URL items to the server; they get created (if they don't already exist) in case of DiscoveredURLItems or updated if KnownURLItems *
         * </pre>
         */
        public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> putURLs(
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>
                        responseObserver) {
            return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
                    getChannel().newCall(getPutURLsMethod(), getCallOptions()), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Return stats for a specific queue or the whole crawl if the value if empty or null *
         * </pre>
         */
        public void getStats(
                crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getGetStatsMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Delete the queue based on the key in parameter, returns the number of URLs removed this way *
         * </pre>
         */
        public void deleteQueue(
                crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getDeleteQueueMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch
         * 1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time
         * indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance.
         * </pre>
         */
        public void blockQueueUntil(
                crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getBlockQueueUntilMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. *
         * </pre>
         */
        public void setActive(
                crawlercommons.urlfrontier.Urlfrontier.Boolean request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getSetActiveMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) *
         * </pre>
         */
        public void getActive(
                crawlercommons.urlfrontier.Urlfrontier.Empty request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Boolean>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getGetActiveMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Set a delay from a given queue.
         * No URLs will be obtained via GetURLs for this queue until the number of seconds specified has
         * elapsed since the last time URLs were retrieved.
         * Usually informed by the delay setting of robots.txt.
         * </pre>
         */
        public void setDelay(
                crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getSetDelayMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }
    }

    /** */
    public static final class URLFrontierBlockingStub
            extends io.grpc.stub.AbstractBlockingStub<URLFrontierBlockingStub> {
        private URLFrontierBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected URLFrontierBlockingStub build(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new URLFrontierBlockingStub(channel, callOptions);
        }

        /**
         *
         *
         * <pre>
         * * Return the list of crawls handled by the frontier *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.StringList listCrawls(
                crawlercommons.urlfrontier.Urlfrontier.Empty request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getListCrawlsMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Delete an entire crawl, returns the number of URLs removed this way *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Integer deleteCrawl(
                crawlercommons.urlfrontier.Urlfrontier.CrawlID request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getDeleteCrawlMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Return a list of queues for a specific crawl. Can chose whether to include inactive queues (a queue is active if it has URLs due for fetching);
         * by default the service will return up to 100 results from offset 0 and exclude inactive queues.*
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.QueueList listQueues(
                crawlercommons.urlfrontier.Urlfrontier.Pagination request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getListQueuesMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Stream URLs due for fetching from M queues with up to N items per queue *
         * </pre>
         */
        public java.util.Iterator<crawlercommons.urlfrontier.Urlfrontier.URLInfo> getURLs(
                crawlercommons.urlfrontier.Urlfrontier.GetParams request) {
            return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
                    getChannel(), getGetURLsMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Return stats for a specific queue or the whole crawl if the value if empty or null *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Stats getStats(
                crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getGetStatsMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Delete the queue based on the key in parameter, returns the number of URLs removed this way *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Integer deleteQueue(
                crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getDeleteQueueMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch
         * 1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time
         * indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance.
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Empty blockQueueUntil(
                crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getBlockQueueUntilMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Empty setActive(
                crawlercommons.urlfrontier.Urlfrontier.Boolean request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getSetActiveMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Boolean getActive(
                crawlercommons.urlfrontier.Urlfrontier.Empty request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getGetActiveMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Set a delay from a given queue.
         * No URLs will be obtained via GetURLs for this queue until the number of seconds specified has
         * elapsed since the last time URLs were retrieved.
         * Usually informed by the delay setting of robots.txt.
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Empty setDelay(
                crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getSetDelayMethod(), getCallOptions(), request);
        }
    }

    /** */
    public static final class URLFrontierFutureStub
            extends io.grpc.stub.AbstractFutureStub<URLFrontierFutureStub> {
        private URLFrontierFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected URLFrontierFutureStub build(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new URLFrontierFutureStub(channel, callOptions);
        }

        /**
         *
         *
         * <pre>
         * * Return the list of crawls handled by the frontier *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.StringList>
                listCrawls(crawlercommons.urlfrontier.Urlfrontier.Empty request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getListCrawlsMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Delete an entire crawl, returns the number of URLs removed this way *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Integer>
                deleteCrawl(crawlercommons.urlfrontier.Urlfrontier.CrawlID request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getDeleteCrawlMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Return a list of queues for a specific crawl. Can chose whether to include inactive queues (a queue is active if it has URLs due for fetching);
         * by default the service will return up to 100 results from offset 0 and exclude inactive queues.*
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.QueueList>
                listQueues(crawlercommons.urlfrontier.Urlfrontier.Pagination request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getListQueuesMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Return stats for a specific queue or the whole crawl if the value if empty or null *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Stats>
                getStats(crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getGetStatsMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Delete the queue based on the key in parameter, returns the number of URLs removed this way *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Integer>
                deleteQueue(crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getDeleteQueueMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch
         * 1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time
         * indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance.
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                blockQueueUntil(crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getBlockQueueUntilMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                setActive(crawlercommons.urlfrontier.Urlfrontier.Boolean request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getSetActiveMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Boolean>
                getActive(crawlercommons.urlfrontier.Urlfrontier.Empty request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getGetActiveMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Set a delay from a given queue.
         * No URLs will be obtained via GetURLs for this queue until the number of seconds specified has
         * elapsed since the last time URLs were retrieved.
         * Usually informed by the delay setting of robots.txt.
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                setDelay(crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getSetDelayMethod(), getCallOptions()), request);
        }
    }

    private static final int METHODID_LIST_CRAWLS = 0;
    private static final int METHODID_DELETE_CRAWL = 1;
    private static final int METHODID_LIST_QUEUES = 2;
    private static final int METHODID_GET_URLS = 3;
    private static final int METHODID_GET_STATS = 4;
    private static final int METHODID_DELETE_QUEUE = 5;
    private static final int METHODID_BLOCK_QUEUE_UNTIL = 6;
    private static final int METHODID_SET_ACTIVE = 7;
    private static final int METHODID_GET_ACTIVE = 8;
    private static final int METHODID_SET_DELAY = 9;
    private static final int METHODID_PUT_URLS = 10;

    private static final class MethodHandlers<Req, Resp>
            implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
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
                case METHODID_LIST_CRAWLS:
                    serviceImpl.listCrawls(
                            (crawlercommons.urlfrontier.Urlfrontier.Empty) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.StringList>)
                                    responseObserver);
                    break;
                case METHODID_DELETE_CRAWL:
                    serviceImpl.deleteCrawl(
                            (crawlercommons.urlfrontier.Urlfrontier.CrawlID) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Integer>)
                                    responseObserver);
                    break;
                case METHODID_LIST_QUEUES:
                    serviceImpl.listQueues(
                            (crawlercommons.urlfrontier.Urlfrontier.Pagination) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.QueueList>)
                                    responseObserver);
                    break;
                case METHODID_GET_URLS:
                    serviceImpl.getURLs(
                            (crawlercommons.urlfrontier.Urlfrontier.GetParams) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.URLInfo>)
                                    responseObserver);
                    break;
                case METHODID_GET_STATS:
                    serviceImpl.getStats(
                            (crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Stats>)
                                    responseObserver);
                    break;
                case METHODID_DELETE_QUEUE:
                    serviceImpl.deleteQueue(
                            (crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Integer>)
                                    responseObserver);
                    break;
                case METHODID_BLOCK_QUEUE_UNTIL:
                    serviceImpl.blockQueueUntil(
                            (crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>)
                                    responseObserver);
                    break;
                case METHODID_SET_ACTIVE:
                    serviceImpl.setActive(
                            (crawlercommons.urlfrontier.Urlfrontier.Boolean) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>)
                                    responseObserver);
                    break;
                case METHODID_GET_ACTIVE:
                    serviceImpl.getActive(
                            (crawlercommons.urlfrontier.Urlfrontier.Empty) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Boolean>)
                                    responseObserver);
                    break;
                case METHODID_SET_DELAY:
                    serviceImpl.setDelay(
                            (crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>)
                                    responseObserver);
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
                    return (io.grpc.stub.StreamObserver<Req>)
                            serviceImpl.putURLs(
                                    (io.grpc.stub.StreamObserver<
                                                    crawlercommons.urlfrontier.Urlfrontier.String>)
                                            responseObserver);
                default:
                    throw new AssertionError();
            }
        }
    }

    private abstract static class URLFrontierBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoFileDescriptorSupplier,
                    io.grpc.protobuf.ProtoServiceDescriptorSupplier {
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
                    serviceDescriptor =
                            result =
                                    io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                                            .setSchemaDescriptor(
                                                    new URLFrontierFileDescriptorSupplier())
                                            .addMethod(getListCrawlsMethod())
                                            .addMethod(getDeleteCrawlMethod())
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
