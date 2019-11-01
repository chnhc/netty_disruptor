import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;

public class testProto {

    public static void main(String[] args) throws Exception {
        String s = "斯科拉快速对";
        Search.NetworkData networkData = Search.NetworkData
                .newBuilder()
                .setData(s)
                .setTypeValue(Search.NetworkData.Encryption_Type.TYPE_TWO_VALUE)
                .build();

        Search.SearchRequest request = Search.SearchRequest
                .newBuilder()
                .addQuery("你好1")
                .addQuery("你好2")
                .addQuery("你好3")
                .setPageNumber(1)
                .setRequestPerPage(1)
                .setData(networkData)
                .build();

        System.out.println(request);
        System.out.println("----------------------------------");

        Search.SearchRequest request1 = Search.SearchRequest.parseFrom(request.toByteArray());

        System.out.println(request1.getQueryList().asByteStringList());

        System.out.println(Search.NetworkData.Encryption_Type.TYPE_TWO);

        System.out.println("----------------------------------");

        Search.Response response = Search.Response
                .newBuilder()
                .setData(Any.pack(networkData))
                .build();

        System.out.println(JsonFormat.printToString(response));

        System.out.println("----------------------------------");

        Search.Response.Builder response2 = Search.Response
                .newBuilder();

        //JsonFormat.merge(JsonFormat.printToString(response), response2);
        Search.NetworkData response1 = response.getData().unpack(Search.NetworkData.class);
        System.out.println(response1.getData());

        //Search.NetworkData response1 = response.getData().unpack(Search.NetworkData.class);
        //System.out.println(response1.getData().toStringUtf8());




        System.out.println("----------------------------------");
        System.out.println(JsonFormat.printToString(request));


        Search.SearchRequest.Builder request2 = Search.SearchRequest
                .newBuilder();

        JsonFormat.merge(JsonFormat.printToString(request), request2);
        System.out.println(request2.build().getData().getData());


        System.out.println("----------------------------------");

        /*

            // protobuf 转 json
            Message.Builder message = Message.newBuilder();
            String json = JsonFormat.printToString(message.build());

            //json 转 protobuf
            try {
                JsonFormat.merge(json, message);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        */
    }

}
