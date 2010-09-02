package org.vimeoid;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vimeoid.connection.JsonObjectsCursor;
import org.vimeoid.connection.JsonOverHttp;
import org.vimeoid.connection.VimeoApiUtils;
import org.vimeoid.connection.simple.ContentType;
import org.vimeoid.dto.simple.Action;
import org.vimeoid.dto.simple.AlbumInfo;
import org.vimeoid.dto.simple.ChannelInfo;
import org.vimeoid.dto.simple.GroupInfo;
import org.vimeoid.dto.simple.UserInfo;
import org.vimeoid.dto.simple.Video;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * <dl>
 * <dt>Project:</dt> <dd>vimeoid</dd>
 * <dt>Package:</dt> <dd>org.vimeoid.providers</dd>
 * </dl>
 *
 * <code>VimeoSimpleApiProvider</code>
 *
 * <p>Description</p>
 * 
 * @author Ulric Wilfred <shaman.sir@gmail.com>
 * @date Aug 20, 2010 5:29:21 PM 
 *
 */

public class VimeoSimpleApiProvider extends ContentProvider {
    
    public static final String AUTHORITY = "org.vimeoid.simple.provider";
    
    public static final String TAG = "VimeoSimpleApiProvider";
    
    public static final Uri BASE_URI = new Uri.Builder().scheme("content").authority(VimeoSimpleApiProvider.AUTHORITY).build();
    
    private static final UriMatcher uriMatcher;
    
    private static final int ACTIONS_LIST_URI_TYPE   = 1;
    private static final int ACTION_SINGLE_URI_TYPE  = 2;
    private static final int ALBUMS_LIST_URI_TYPE    = 3;
    private static final int ALBUM_SINGLE_URI_TYPE   = 4;
    private static final int CHANNELS_LIST_URI_TYPE  = 5;
    private static final int CHANNEL_SINGLE_URI_TYPE = 6;
    private static final int GROUPS_LIST_URI_TYPE    = 7;
    private static final int GROUP_SINGLE_URI_TYPE   = 8;
    private static final int USERS_LIST_URI_TYPE     = 9;
    private static final int USER_SINGLE_URI_TYPE    = 10;
    private static final int VIDEOS_LIST_URI_TYPE    = 11;
    private static final int VIDEO_SINGLE_URI_TYPE   = 12;    
    
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        
        uriMatcher.addURI(AUTHORITY, "user/*/info",     USER_SINGLE_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "user/*/videos",   VIDEOS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "user/*/likes",    VIDEOS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "user/*/appears",  VIDEOS_LIST_URI_TYPE); // videos where user appears
        uriMatcher.addURI(AUTHORITY, "user/*/all",      VIDEOS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "user/*/subscr",   VIDEOS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "user/*/albums",   ALBUMS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "user/*/channels", CHANNELS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "user/*/groups",   GROUPS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "user/*/ccreated", VIDEOS_LIST_URI_TYPE); // videos that user's contacts created
        uriMatcher.addURI(AUTHORITY, "user/*/clikes",   VIDEOS_LIST_URI_TYPE); // videos that user's contacts liked
        
        uriMatcher.addURI(AUTHORITY, "video/#", VIDEO_SINGLE_URI_TYPE);
        
        uriMatcher.addURI(AUTHORITY, "group/*/info",   GROUP_SINGLE_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "group/*/videos", VIDEOS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "group/*/users",  USERS_LIST_URI_TYPE);
        
        uriMatcher.addURI(AUTHORITY, "channel/*/info",   CHANNEL_SINGLE_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "channel/*/videos", VIDEOS_LIST_URI_TYPE);
        
        uriMatcher.addURI(AUTHORITY, "album/#/info",   ALBUM_SINGLE_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "album/#/videos", VIDEOS_LIST_URI_TYPE);
        
        uriMatcher.addURI(AUTHORITY, "activity/*/did",       ACTIONS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "activity/*/happened",  ACTIONS_LIST_URI_TYPE);
        uriMatcher.addURI(AUTHORITY, "activity/*/cdid",      ACTIONS_LIST_URI_TYPE); // contacts did
        uriMatcher.addURI(AUTHORITY, "activity/*/chappened", ACTIONS_LIST_URI_TYPE); // happened to contacts
        uriMatcher.addURI(AUTHORITY, "activity/*/edid",      ACTIONS_LIST_URI_TYPE); // everyone did
    }

    /**
     * This method is not supported in <code>VimeoSimpleApiProvider</code>
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Deletion of something in not supported in VimeoSimpleApiProvider");
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ACTIONS_LIST_URI_TYPE:   return Action.CONTENT_TYPE;
            case ACTION_SINGLE_URI_TYPE:  return Action.CONTENT_ITEM_TYPE;
            case ALBUMS_LIST_URI_TYPE:    return AlbumInfo.CONTENT_TYPE;
            case ALBUM_SINGLE_URI_TYPE:   return AlbumInfo.CONTENT_ITEM_TYPE;
            case CHANNELS_LIST_URI_TYPE:  return ChannelInfo.CONTENT_TYPE;
            case CHANNEL_SINGLE_URI_TYPE: return ChannelInfo.CONTENT_ITEM_TYPE;
            case GROUPS_LIST_URI_TYPE:    return GroupInfo.CONTENT_TYPE;
            case GROUP_SINGLE_URI_TYPE:   return GroupInfo.CONTENT_ITEM_TYPE;
            case USERS_LIST_URI_TYPE:     return UserInfo.CONTENT_TYPE;
            case USER_SINGLE_URI_TYPE:    return UserInfo.CONTENT_ITEM_TYPE;
            case VIDEOS_LIST_URI_TYPE:    return Video.CONTENT_TYPE;
            case VIDEO_SINGLE_URI_TYPE:   return Video.CONTENT_ITEM_TYPE;
            default: throw new IllegalArgumentException("Unknown URI type: " + uri);
        }
    }

    public static ContentType getReturnedContentType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ACTIONS_LIST_URI_TYPE:
            case ACTION_SINGLE_URI_TYPE:  return ContentType.ACTIVITY;
            case ALBUMS_LIST_URI_TYPE:
            case ALBUM_SINGLE_URI_TYPE:   return ContentType.ALBUM;
            case CHANNELS_LIST_URI_TYPE:
            case CHANNEL_SINGLE_URI_TYPE: return ContentType.CHANNEL;
            case GROUPS_LIST_URI_TYPE:
            case GROUP_SINGLE_URI_TYPE:   return ContentType.GROUP;
            case USERS_LIST_URI_TYPE:
            case USER_SINGLE_URI_TYPE:    return ContentType.USER;
            case VIDEOS_LIST_URI_TYPE:
            case VIDEO_SINGLE_URI_TYPE:   return ContentType.VIDEO;
            default: throw new IllegalArgumentException("Unknown URI type: " + uri);
        }        
    }
    
    public static boolean getReturnsMultipleResults(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ACTIONS_LIST_URI_TYPE:
            case ALBUMS_LIST_URI_TYPE:
            case CHANNELS_LIST_URI_TYPE:
            case GROUPS_LIST_URI_TYPE:
            case USERS_LIST_URI_TYPE:
            case VIDEOS_LIST_URI_TYPE:    return true;            
            case ACTION_SINGLE_URI_TYPE:  
            case ALBUM_SINGLE_URI_TYPE:
            case CHANNEL_SINGLE_URI_TYPE:
            case GROUP_SINGLE_URI_TYPE:
            case USER_SINGLE_URI_TYPE:
            case VIDEO_SINGLE_URI_TYPE:   return false;
            default: throw new IllegalArgumentException("Unknown URI type: " + uri);
        }        
    }    
    
    /**
     * This method is not supported in <code>VimeoSimpleApiProvider</code>
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Insertion of something in not supported in VimeoSimpleApiProvider");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(Uri contentUri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if ((selection != null) || (selectionArgs != null))
            throw new UnsupportedOperationException("SQL Where-selections are not supported in VimeoSimpleApiProvider, please use URI to filter the selection query");
        if (sortOrder != null) throw new UnsupportedOperationException("SQL-styled sorting is not supported in VimeoSimpleApiProvider, please use URI parameters to specify sorting order (if supported by the method)");
        if (projection == null) throw new IllegalArgumentException("Please specify projection, at least empty one");
        final URI vimeoApiUri = VimeoApiUtils.resolveUriForSimpleApi(contentUri);
        try {
            if (getReturnsMultipleResults(contentUri)) {
                final JSONArray jsonArr = JsonOverHttp.use().askForArray(vimeoApiUri);
                return new JsonObjectsCursor(jsonArr, projection);
            } else {
                final JSONObject jsonObj = JsonOverHttp.use().askForObject(vimeoApiUri);
                return new JsonObjectsCursor(jsonObj, projection);
            }
        } catch (ClientProtocolException cpe) {
            Log.e(TAG, "Client protocol exception" + cpe.getLocalizedMessage());
            cpe.printStackTrace();
        } catch (JSONException jsone) {
            Log.e(TAG, "JSON parsing exception " + jsone.getLocalizedMessage());
            jsone.printStackTrace();
        } catch (IOException ioe) {
            Log.e(TAG, "Connection/IO exception " + ioe.getLocalizedMessage());
            ioe.printStackTrace();
        }
        return null;
    }

    /**
     * This method is not supported in <code>VimeoSimpleApiProvider</code>
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("Updation of something in not supported in VimeoSimpleApiProvider");
    }

}
