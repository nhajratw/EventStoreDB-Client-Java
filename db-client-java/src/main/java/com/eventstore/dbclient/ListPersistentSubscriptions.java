package com.eventstore.dbclient;

import com.eventstore.dbclient.proto.persistentsubscriptions.PersistentSubscriptionsGrpc;
import com.eventstore.dbclient.proto.shared.Shared;
import com.fasterxml.jackson.databind.JsonNode;
import com.eventstore.dbclient.proto.persistentsubscriptions.Persistent;
import com.google.protobuf.ByteString;
import io.grpc.stub.MetadataUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.eventstore.dbclient.HttpUtils.*;

public final class ListPersistentSubscriptions {
    public static CompletableFuture<List<PersistentSubscriptionInfo>> execute(GrpcClient client, ListPersistentSubscriptionsOptions options, String stream) {
        return client.runWithArgs(args -> {
            CompletableFuture<List<PersistentSubscriptionInfo>> result = new CompletableFuture<>();

            if (stream.equals("$all") && !args.supportFeature(FeatureFlags.PERSISTENT_SUBSCRIPTION_TO_ALL)) {
                result.completeExceptionally(new UnsupportedFeature());
                return result;
            }

            if (args.supportFeature(FeatureFlags.PERSISTENT_SUBSCRIPTION_MANAGEMENT)) {
                Persistent.ListReq.Options.Builder optionsBuilder = Persistent.ListReq.Options.newBuilder();

                if (stream.equals("")) {
                    optionsBuilder.setListAllSubscriptions(Shared.Empty.getDefaultInstance());
                } else if (stream.equals("$all")) {
                    optionsBuilder.setListForStream(Persistent.ListReq.StreamOption.newBuilder().setAll(Shared.Empty.getDefaultInstance()));
                } else {
                    optionsBuilder.setListForStream(Persistent.ListReq.StreamOption.newBuilder().setStream(Shared.StreamIdentifier.newBuilder().setStreamName(ByteString.copyFromUtf8(stream))));
                }

                Persistent.ListReq req = Persistent.ListReq.newBuilder()
                        .setOptions(optionsBuilder)
                        .build();

                PersistentSubscriptionsGrpc.PersistentSubscriptionsStub stub = MetadataUtils
                        .attachHeaders(PersistentSubscriptionsGrpc.newStub(args.getChannel()), options.getMetadata());

                stub.list(req, GrpcUtils.convertSingleResponse(result, resp -> {
                    List<PersistentSubscriptionInfo> infos = new ArrayList<>();

                    for (Persistent.SubscriptionInfo wire : resp.getSubscriptionsList()) {
                        infos.add(parseInfoFromWire(wire));
                    }

                    return infos;
                }));
            } else {
                String suffix = "";

                if (!stream.equals("")) {
                    suffix = String.format("/%s", urlEncode(stream));
                }

                HttpURLConnection http = args.getHttpConnection(options, client.settings, String.format("/subscriptions%s", suffix));
                try {
                    http.setRequestMethod("GET");

                    Exception error = checkForError(http.getResponseCode());
                    if (error != null) {
                        result.completeExceptionally(error);
                    } else {
                        String content = readContent(http);
                        List<PersistentSubscriptionInfo> ps = new ArrayList<>();

                        for (JsonNode node : getObjectMapper().readTree(content)) {
                            ps.add(parseSubscriptionInfo(node));
                        }
                        result.complete(ps);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    http.disconnect();
                }
            }

            return result;
        });
    }
}