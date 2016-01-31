package co.paralleluniverse.fibers.okhttp.test.utils;

import co.paralleluniverse.fibers.okhttp.FiberOkHttpClient;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;

final public class FiberOkHttpClientTestWrapper extends FiberOkHttpClient {
    @Override
    public Call newCall(Request request) {
        return new FiberCallTestWrapper(this, super.newCall(request), request);
    }
}
