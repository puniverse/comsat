package co.paralleluniverse.fibers.okhttp.test.apache;

import co.paralleluniverse.fibers.okhttp.FiberOkHttpUtil;
import okhttp3.apache.OkApacheClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class OkApacheTestWrapper
{
    private OkApacheClient underlying;

    public OkApacheTestWrapper(OkApacheClient underlying) {
        this.underlying = underlying;
    }

    public HttpResponse execute(HttpRequestBase request) throws IOException, InterruptedException {
        return FiberOkHttpUtil.executeInFiber(underlying, request);
    }
}
