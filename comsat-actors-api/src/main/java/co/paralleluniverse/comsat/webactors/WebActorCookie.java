package co.paralleluniverse.comsat.webactors;

import java.util.Date;


public interface WebActorCookie {

    String getName();

    String getValue();

    WebActorCookie setValue(final String value);

    String getPath();

    WebActorCookie setPath(final String path);

    String getDomain();

    WebActorCookie setDomain(final String domain);

    Integer getMaxAge();

    WebActorCookie setMaxAge(final Integer maxAge);

    boolean isDiscard();

    WebActorCookie setDiscard(final boolean discard);

    boolean isSecure();

    WebActorCookie setSecure(final boolean secure);

    int getVersion();

    WebActorCookie setVersion(final int version);

    boolean isHttpOnly();

    WebActorCookie setHttpOnly(final boolean httpOnly);

    Date getExpires();

    WebActorCookie setExpires(final Date expires);

    String getComment();

    WebActorCookie setComment(final String comment);
}
