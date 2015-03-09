/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.okhttp;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

/**
 * Fiber-blocking OkHttp's {@link OkHttpClient} implementation.
 *
 * @author circlespainter
 */
public class FiberOkHttpClient extends OkHttpClient {

    @Override
    public Call newCall(Request request) {
        return new FiberCall(this, request);
    }
}
