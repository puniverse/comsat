package co.paralleluniverse.embedded;


public abstract class AbstractEmbeddedServer implements EmbeddedServer {
    protected int port= 8080;
    protected int nThreads = 10;
    protected int maxConn = 1000;
    
    @Override
    public EmbeddedServer setPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public EmbeddedServer setNumThreads(int nThreads) {
        this.nThreads = nThreads;
        return this;
    }

    @Override
    public EmbeddedServer setMaxConnections(int maxConn) {
        this.maxConn = maxConn;
        return this;
    }
}
