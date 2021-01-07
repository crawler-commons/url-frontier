package crawlercommons.urlfrontier.client;

import java.util.LinkedList;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.GetParams.Builder;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {

	public static void main(String[] args) throws InvalidProtocolBufferException {
		int i = 0, j;
		String arg;

		String host = "localhost";
		int port = 7071;

		List<String> commands = new LinkedList<>();

		while (i < args.length) {
			arg = args[i++];

			if (arg.equals("-host")) {
				if (i < args.length)
					host = args[i++];
				else {
					System.err.println("-host requires a value");
					return;
				}
				continue;
			}

			if (arg.equals("-port")) {
				if (i < args.length)
					port = Integer.parseInt(args[i++]);
				else {
					System.err.println("-port requires a value");
					return;
				}
				continue;
			}

			commands.add(arg);
		}

		ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
		URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

		if (commands.isEmpty()) {
			System.err.println("command missing");
			return;
		}

		String command = commands.remove(0);

		if (command.equalsIgnoreCase("ListQueues")) {
			Builder builder = GetParams.newBuilder();
			if (!commands.isEmpty()) {
				String json_string = commands.get(0);
				JsonFormat.parser().merge(json_string, builder);
			}
			StringList list = blockingFrontier.listQueues(builder.build());
			for (i = 0; i < list.getStringCount(); i++) {
				System.out.println(list.getString(i));
			}
			return;
		}

		if (command.equalsIgnoreCase("stats")) {
			crawlercommons.urlfrontier.Urlfrontier.String.Builder builder = crawlercommons.urlfrontier.Urlfrontier.String
					.newBuilder();
			if (!commands.isEmpty()) {
				String json_string = commands.get(0);
				JsonFormat.parser().merge(json_string, builder);
			}
			Stats s = blockingFrontier.stats(builder.build());
			System.out.println("Number of queues: " + s.getNumberOfQueues());
			System.out.println("In process: " + s.getInProcess());
			return;
		}

		if (command.equalsIgnoreCase("inject")) {
			// argument is a file
			// read each line and send the content as updates
		}

	}

}
