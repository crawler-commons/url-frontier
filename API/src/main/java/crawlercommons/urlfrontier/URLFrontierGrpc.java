package crawlercommons.urlfrontier;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/** */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.50.2)",
        comments = "Source: urlfrontier.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class URLFrontierGrpc {

    private URLFrontierGrpc() {}

    public static final String SERVICE_NAME = "urlfrontier.URLFrontier";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Empty,
                    crawlercommons.urlfrontier.Urlfrontier.StringList>
            getListNodesMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "ListNodes",
            requestType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.StringList.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Empty,
                    crawlercommons.urlfrontier.Urlfrontier.StringList>
            getListNodesMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.Empty,
                        crawlercommons.urlfrontier.Urlfrontier.StringList>
                getListNodesMethod;
        if ((getListNodesMethod = URLFrontierGrpc.getListNodesMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getListNodesMethod = URLFrontierGrpc.getListNodesMethod) == null) {
                    URLFrontierGrpc.getListNodesMethod =
                            getListNodesMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.Empty,
                                                    crawlercommons.urlfrontier.Urlfrontier
                                                            .StringList>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "ListNodes"))
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
                                                            "ListNodes"))
                                            .build();
                }
            }
        }
        return getListNodesMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Local,
                    crawlercommons.urlfrontier.Urlfrontier.StringList>
            getListCrawlsMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "ListCrawls",
            requestType = crawlercommons.urlfrontier.Urlfrontier.Local.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.StringList.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Local,
                    crawlercommons.urlfrontier.Urlfrontier.StringList>
            getListCrawlsMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.Local,
                        crawlercommons.urlfrontier.Urlfrontier.StringList>
                getListCrawlsMethod;
        if ((getListCrawlsMethod = URLFrontierGrpc.getListCrawlsMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getListCrawlsMethod = URLFrontierGrpc.getListCrawlsMethod) == null) {
                    URLFrontierGrpc.getListCrawlsMethod =
                            getListCrawlsMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.Local,
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
                                                                    .Local.getDefaultInstance()))
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
                    crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage,
                    crawlercommons.urlfrontier.Urlfrontier.Long>
            getDeleteCrawlMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "DeleteCrawl",
            requestType = crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Long.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage,
                    crawlercommons.urlfrontier.Urlfrontier.Long>
            getDeleteCrawlMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage,
                        crawlercommons.urlfrontier.Urlfrontier.Long>
                getDeleteCrawlMethod;
        if ((getDeleteCrawlMethod = URLFrontierGrpc.getDeleteCrawlMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getDeleteCrawlMethod = URLFrontierGrpc.getDeleteCrawlMethod) == null) {
                    URLFrontierGrpc.getDeleteCrawlMethod =
                            getDeleteCrawlMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier
                                                            .DeleteCrawlMessage,
                                                    crawlercommons.urlfrontier.Urlfrontier.Long>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "DeleteCrawl"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .DeleteCrawlMessage
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Long.getDefaultInstance()))
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
                    crawlercommons.urlfrontier.Urlfrontier.AckMessage>
            getPutURLsMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "PutURLs",
            requestType = crawlercommons.urlfrontier.Urlfrontier.URLItem.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.AckMessage.class,
            methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.URLItem,
                    crawlercommons.urlfrontier.Urlfrontier.AckMessage>
            getPutURLsMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.URLItem,
                        crawlercommons.urlfrontier.Urlfrontier.AckMessage>
                getPutURLsMethod;
        if ((getPutURLsMethod = URLFrontierGrpc.getPutURLsMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getPutURLsMethod = URLFrontierGrpc.getPutURLsMethod) == null) {
                    URLFrontierGrpc.getPutURLsMethod =
                            getPutURLsMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.URLItem,
                                                    crawlercommons.urlfrontier.Urlfrontier
                                                            .AckMessage>
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
                                                                    .AckMessage
                                                                    .getDefaultInstance()))
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
                    crawlercommons.urlfrontier.Urlfrontier.Long>
            getDeleteQueueMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "DeleteQueue",
            requestType = crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Long.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams,
                    crawlercommons.urlfrontier.Urlfrontier.Long>
            getDeleteQueueMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams,
                        crawlercommons.urlfrontier.Urlfrontier.Long>
                getDeleteQueueMethod;
        if ((getDeleteQueueMethod = URLFrontierGrpc.getDeleteQueueMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getDeleteQueueMethod = URLFrontierGrpc.getDeleteQueueMethod) == null) {
                    URLFrontierGrpc.getDeleteQueueMethod =
                            getDeleteQueueMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier
                                                            .QueueWithinCrawlParams,
                                                    crawlercommons.urlfrontier.Urlfrontier.Long>
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
                                                                    .Long.getDefaultInstance()))
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
                    crawlercommons.urlfrontier.Urlfrontier.Active,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetActiveMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "SetActive",
            requestType = crawlercommons.urlfrontier.Urlfrontier.Active.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Active,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetActiveMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.Active,
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                getSetActiveMethod;
        if ((getSetActiveMethod = URLFrontierGrpc.getSetActiveMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getSetActiveMethod = URLFrontierGrpc.getSetActiveMethod) == null) {
                    URLFrontierGrpc.getSetActiveMethod =
                            getSetActiveMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.Active,
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
                                                                    .Active.getDefaultInstance()))
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
                    crawlercommons.urlfrontier.Urlfrontier.Local,
                    crawlercommons.urlfrontier.Urlfrontier.Boolean>
            getGetActiveMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "GetActive",
            requestType = crawlercommons.urlfrontier.Urlfrontier.Local.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Boolean.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.Local,
                    crawlercommons.urlfrontier.Urlfrontier.Boolean>
            getGetActiveMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.Local,
                        crawlercommons.urlfrontier.Urlfrontier.Boolean>
                getGetActiveMethod;
        if ((getGetActiveMethod = URLFrontierGrpc.getGetActiveMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getGetActiveMethod = URLFrontierGrpc.getGetActiveMethod) == null) {
                    URLFrontierGrpc.getGetActiveMethod =
                            getGetActiveMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.Local,
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
                                                                    .Local.getDefaultInstance()))
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

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.LogLevelParams,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetLogLevelMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "SetLogLevel",
            requestType = crawlercommons.urlfrontier.Urlfrontier.LogLevelParams.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.LogLevelParams,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetLogLevelMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.LogLevelParams,
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                getSetLogLevelMethod;
        if ((getSetLogLevelMethod = URLFrontierGrpc.getSetLogLevelMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getSetLogLevelMethod = URLFrontierGrpc.getSetLogLevelMethod) == null) {
                    URLFrontierGrpc.getSetLogLevelMethod =
                            getSetLogLevelMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier.LogLevelParams,
                                                    crawlercommons.urlfrontier.Urlfrontier.Empty>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "SetLogLevel"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .LogLevelParams
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Empty.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "SetLogLevel"))
                                            .build();
                }
            }
        }
        return getSetLogLevelMethod;
    }

    private static volatile io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetCrawlLimitMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "SetCrawlLimit",
            requestType = crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams.class,
            responseType = crawlercommons.urlfrontier.Urlfrontier.Empty.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<
                    crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams,
                    crawlercommons.urlfrontier.Urlfrontier.Empty>
            getSetCrawlLimitMethod() {
        io.grpc.MethodDescriptor<
                        crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams,
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                getSetCrawlLimitMethod;
        if ((getSetCrawlLimitMethod = URLFrontierGrpc.getSetCrawlLimitMethod) == null) {
            synchronized (URLFrontierGrpc.class) {
                if ((getSetCrawlLimitMethod = URLFrontierGrpc.getSetCrawlLimitMethod) == null) {
                    URLFrontierGrpc.getSetCrawlLimitMethod =
                            getSetCrawlLimitMethod =
                                    io.grpc.MethodDescriptor
                                            .<crawlercommons.urlfrontier.Urlfrontier
                                                            .CrawlLimitParams,
                                                    crawlercommons.urlfrontier.Urlfrontier.Empty>
                                                    newBuilder()
                                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                            .setFullMethodName(
                                                    generateFullMethodName(
                                                            SERVICE_NAME, "SetCrawlLimit"))
                                            .setSampledToLocalTracing(true)
                                            .setRequestMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .CrawlLimitParams
                                                                    .getDefaultInstance()))
                                            .setResponseMarshaller(
                                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                                            crawlercommons.urlfrontier.Urlfrontier
                                                                    .Empty.getDefaultInstance()))
                                            .setSchemaDescriptor(
                                                    new URLFrontierMethodDescriptorSupplier(
                                                            "SetCrawlLimit"))
                                            .build();
                }
            }
        }
        return getSetCrawlLimitMethod;
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
         * * Return the list of nodes forming the cluster the current node belongs to *
         * </pre>
         */
        public void listNodes(
                crawlercommons.urlfrontier.Urlfrontier.Empty request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getListNodesMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Return the list of crawls handled by the frontier(s) *
         * </pre>
         */
        public void listCrawls(
                crawlercommons.urlfrontier.Urlfrontier.Local request,
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
                crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
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
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage>
                        responseObserver) {
            return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(
                    getPutURLsMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Return stats for a specific queue or an entire crawl. Does not aggregate the stats across different crawlids. *
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
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
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
                crawlercommons.urlfrontier.Urlfrontier.Active request,
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
                crawlercommons.urlfrontier.Urlfrontier.Local request,
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

        /**
         *
         *
         * <pre>
         * * Overrides the log level for a given package *
         * </pre>
         */
        public void setLogLevel(
                crawlercommons.urlfrontier.Urlfrontier.LogLevelParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getSetLogLevelMethod(), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Sets crawl limit for domain *
         * </pre>
         */
        public void setCrawlLimit(
                crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
                    getSetCrawlLimitMethod(), responseObserver);
        }

        @java.lang.Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                    .addMethod(
                            getListNodesMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.Empty,
                                            crawlercommons.urlfrontier.Urlfrontier.StringList>(
                                            this, METHODID_LIST_NODES)))
                    .addMethod(
                            getListCrawlsMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.Local,
                                            crawlercommons.urlfrontier.Urlfrontier.StringList>(
                                            this, METHODID_LIST_CRAWLS)))
                    .addMethod(
                            getDeleteCrawlMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier
                                                    .DeleteCrawlMessage,
                                            crawlercommons.urlfrontier.Urlfrontier.Long>(
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
                                            crawlercommons.urlfrontier.Urlfrontier.AckMessage>(
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
                                            crawlercommons.urlfrontier.Urlfrontier.Long>(
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
                                            crawlercommons.urlfrontier.Urlfrontier.Active,
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>(
                                            this, METHODID_SET_ACTIVE)))
                    .addMethod(
                            getGetActiveMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.Local,
                                            crawlercommons.urlfrontier.Urlfrontier.Boolean>(
                                            this, METHODID_GET_ACTIVE)))
                    .addMethod(
                            getSetDelayMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams,
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>(
                                            this, METHODID_SET_DELAY)))
                    .addMethod(
                            getSetLogLevelMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.LogLevelParams,
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>(
                                            this, METHODID_SET_LOG_LEVEL)))
                    .addMethod(
                            getSetCrawlLimitMethod(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new MethodHandlers<
                                            crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams,
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>(
                                            this, METHODID_SET_CRAWL_LIMIT)))
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
         * * Return the list of nodes forming the cluster the current node belongs to *
         * </pre>
         */
        public void listNodes(
                crawlercommons.urlfrontier.Urlfrontier.Empty request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getListNodesMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Return the list of crawls handled by the frontier(s) *
         * </pre>
         */
        public void listCrawls(
                crawlercommons.urlfrontier.Urlfrontier.Local request,
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
                crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
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
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage>
                        responseObserver) {
            return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
                    getChannel().newCall(getPutURLsMethod(), getCallOptions()), responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Return stats for a specific queue or an entire crawl. Does not aggregate the stats across different crawlids. *
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
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
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
                crawlercommons.urlfrontier.Urlfrontier.Active request,
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
                crawlercommons.urlfrontier.Urlfrontier.Local request,
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

        /**
         *
         *
         * <pre>
         * * Overrides the log level for a given package *
         * </pre>
         */
        public void setLogLevel(
                crawlercommons.urlfrontier.Urlfrontier.LogLevelParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getSetLogLevelMethod(), getCallOptions()),
                    request,
                    responseObserver);
        }

        /**
         *
         *
         * <pre>
         * * Sets crawl limit for domain *
         * </pre>
         */
        public void setCrawlLimit(
                crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams request,
                io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty>
                        responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getSetCrawlLimitMethod(), getCallOptions()),
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
         * * Return the list of nodes forming the cluster the current node belongs to *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.StringList listNodes(
                crawlercommons.urlfrontier.Urlfrontier.Empty request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getListNodesMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Return the list of crawls handled by the frontier(s) *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.StringList listCrawls(
                crawlercommons.urlfrontier.Urlfrontier.Local request) {
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
        public crawlercommons.urlfrontier.Urlfrontier.Long deleteCrawl(
                crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage request) {
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
         * * Return stats for a specific queue or an entire crawl. Does not aggregate the stats across different crawlids. *
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
        public crawlercommons.urlfrontier.Urlfrontier.Long deleteQueue(
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
                crawlercommons.urlfrontier.Urlfrontier.Active request) {
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
                crawlercommons.urlfrontier.Urlfrontier.Local request) {
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

        /**
         *
         *
         * <pre>
         * * Overrides the log level for a given package *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Empty setLogLevel(
                crawlercommons.urlfrontier.Urlfrontier.LogLevelParams request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getSetLogLevelMethod(), getCallOptions(), request);
        }

        /**
         *
         *
         * <pre>
         * * Sets crawl limit for domain *
         * </pre>
         */
        public crawlercommons.urlfrontier.Urlfrontier.Empty setCrawlLimit(
                crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getSetCrawlLimitMethod(), getCallOptions(), request);
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
         * * Return the list of nodes forming the cluster the current node belongs to *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.StringList>
                listNodes(crawlercommons.urlfrontier.Urlfrontier.Empty request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getListNodesMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Return the list of crawls handled by the frontier(s) *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.StringList>
                listCrawls(crawlercommons.urlfrontier.Urlfrontier.Local request) {
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
                        crawlercommons.urlfrontier.Urlfrontier.Long>
                deleteCrawl(crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage request) {
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
         * * Return stats for a specific queue or an entire crawl. Does not aggregate the stats across different crawlids. *
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
                        crawlercommons.urlfrontier.Urlfrontier.Long>
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
                setActive(crawlercommons.urlfrontier.Urlfrontier.Active request) {
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
                getActive(crawlercommons.urlfrontier.Urlfrontier.Local request) {
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

        /**
         *
         *
         * <pre>
         * * Overrides the log level for a given package *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                setLogLevel(crawlercommons.urlfrontier.Urlfrontier.LogLevelParams request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getSetLogLevelMethod(), getCallOptions()), request);
        }

        /**
         *
         *
         * <pre>
         * * Sets crawl limit for domain *
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<
                        crawlercommons.urlfrontier.Urlfrontier.Empty>
                setCrawlLimit(crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getSetCrawlLimitMethod(), getCallOptions()), request);
        }
    }

    private static final int METHODID_LIST_NODES = 0;
    private static final int METHODID_LIST_CRAWLS = 1;
    private static final int METHODID_DELETE_CRAWL = 2;
    private static final int METHODID_LIST_QUEUES = 3;
    private static final int METHODID_GET_URLS = 4;
    private static final int METHODID_GET_STATS = 5;
    private static final int METHODID_DELETE_QUEUE = 6;
    private static final int METHODID_BLOCK_QUEUE_UNTIL = 7;
    private static final int METHODID_SET_ACTIVE = 8;
    private static final int METHODID_GET_ACTIVE = 9;
    private static final int METHODID_SET_DELAY = 10;
    private static final int METHODID_SET_LOG_LEVEL = 11;
    private static final int METHODID_SET_CRAWL_LIMIT = 12;
    private static final int METHODID_PUT_URLS = 13;

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
                case METHODID_LIST_NODES:
                    serviceImpl.listNodes(
                            (crawlercommons.urlfrontier.Urlfrontier.Empty) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.StringList>)
                                    responseObserver);
                    break;
                case METHODID_LIST_CRAWLS:
                    serviceImpl.listCrawls(
                            (crawlercommons.urlfrontier.Urlfrontier.Local) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.StringList>)
                                    responseObserver);
                    break;
                case METHODID_DELETE_CRAWL:
                    serviceImpl.deleteCrawl(
                            (crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Long>)
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
                                            crawlercommons.urlfrontier.Urlfrontier.Long>)
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
                            (crawlercommons.urlfrontier.Urlfrontier.Active) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>)
                                    responseObserver);
                    break;
                case METHODID_GET_ACTIVE:
                    serviceImpl.getActive(
                            (crawlercommons.urlfrontier.Urlfrontier.Local) request,
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
                case METHODID_SET_LOG_LEVEL:
                    serviceImpl.setLogLevel(
                            (crawlercommons.urlfrontier.Urlfrontier.LogLevelParams) request,
                            (io.grpc.stub.StreamObserver<
                                            crawlercommons.urlfrontier.Urlfrontier.Empty>)
                                    responseObserver);
                    break;
                case METHODID_SET_CRAWL_LIMIT:
                    serviceImpl.setCrawlLimit(
                            (crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams) request,
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
                                                    crawlercommons.urlfrontier.Urlfrontier
                                                            .AckMessage>)
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
                                            .addMethod(getListNodesMethod())
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
                                            .addMethod(getSetLogLevelMethod())
                                            .addMethod(getSetCrawlLimitMethod())
                                            .build();
                }
            }
        }
        return result;
    }
}
