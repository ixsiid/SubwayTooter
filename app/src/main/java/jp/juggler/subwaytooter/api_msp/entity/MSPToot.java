package jp.juggler.subwaytooter.api_msp.entity;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.juggler.subwaytooter.api.entity.TootStatusLike;
import jp.juggler.subwaytooter.table.SavedAccount;
import jp.juggler.subwaytooter.util.DecodeOptions;
import jp.juggler.subwaytooter.util.HTMLDecoder;
import jp.juggler.subwaytooter.util.LogCategory;
import jp.juggler.subwaytooter.util.Utils;
import jp.juggler.subwaytooter.util.WordTrieTree;

public class MSPToot extends TootStatusLike {
	
	private static final LogCategory log = new LogCategory( "MSPToot" );
	
	public static class List extends ArrayList< MSPToot > {
		
	}
	

	private String created_at;
	
	public ArrayList< String > media_attachments;
	// private long msp_id;
	
	@Nullable
	private static MSPToot parse( @NonNull Context context, SavedAccount access_info, JSONObject src ){
		if( src == null ) return null;
		MSPToot dst = new MSPToot();
		
		dst.account = MSPAccount.parseAccount( context, access_info, src.optJSONObject( "account" ) );
		if( dst.account == null ){
			log.e( "missing status account" );
			return null;
		}
		
		dst.json = src;
		
		dst.url = Utils.optStringX( src, "url" );
		dst.host_original = dst.account.getAcctHost();
		dst.host_access = "?";
		dst.id = src.optLong( "id", - 1L );
		
		if( TextUtils.isEmpty( dst.url ) || TextUtils.isEmpty( dst.host_original ) || dst.id == - 1L ){
			log.e( "missing status url or host or id" );
			return null;
		}
		
		dst.created_at = Utils.optStringX( src, "created_at" );
		dst.time_created_at = parseTime( dst.created_at );
		
		JSONArray a = src.optJSONArray( "media_attachments" );
		if( a != null && a.length() > 0 ){
			dst.media_attachments = new ArrayList<>();
			for( int i = 0, ie = a.length() ; i < ie ; ++ i ){
				String sv = Utils.optStringX( a, i );
				if( ! TextUtils.isEmpty( sv ) ){
					dst.media_attachments.add( sv );
				}
			}
		}
		
		// dst.msp_id = src.optLong( "msp_id" );
		dst.sensitive = ( src.optInt( "sensitive", 0 ) != 0 );
		
		dst.setSpoilerText( context, Utils.optStringX( src, "spoiler_text" ) );
		
		dst.content = Utils.optStringX( src, "content" );
		dst.decoded_content = new DecodeOptions()
			.setShort( true )
			.setDecodeEmoji( true )
			.setEmojiMap( dst.emojis )
			.setLinkTag( dst )
			.decodeHTML( context, access_info, dst.content);
		
		return dst;
	}
	
	public static List parseList( @NonNull Context context, SavedAccount access_info, JSONArray array ){
		List list = new List();
		for( int i = 0, ie = array.length() ; i < ie ; ++ i ){
			JSONObject src = array.optJSONObject( i );
			if( src == null ) continue;
			MSPToot item = parse( context, access_info, src );
			if( item == null ) continue;
			list.add( item );
		}
		return list;
	}
	
	private static final Pattern reTime = Pattern.compile( "\\A(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)" );
	
	private static final TimeZone tz_utc = TimeZone.getTimeZone( "UTC" );
	
	private static long parseTime( String strTime ){
		if( ! TextUtils.isEmpty( strTime ) ){
			try{
				Matcher m = reTime.matcher( strTime );
				if( ! m.find() ){
					log.d( "!!invalid time format: %s", strTime );
				}else{
					GregorianCalendar g = new GregorianCalendar( tz_utc );
					g.set(
						Utils.parse_int( m.group( 1 ), 1 ),
						Utils.parse_int( m.group( 2 ), 1 ) - 1,
						Utils.parse_int( m.group( 3 ), 1 ),
						Utils.parse_int( m.group( 4 ), 0 ),
						Utils.parse_int( m.group( 5 ), 0 ),
						Utils.parse_int( m.group( 6 ), 0 )
					);
					g.set( Calendar.MILLISECOND, 500 );
					return g.getTimeInMillis();
				}
			}catch( Throwable ex ){// ParseException,  ArrayIndexOutOfBoundsException
				log.trace( ex );
				log.e( ex, "TootStatus.parseTime failed. src=%s", strTime );
			}
		}
		return 0L;
	}
	
	public boolean checkMuted( @SuppressWarnings("UnusedParameters") @NonNull HashSet< String > muted_app, @NonNull WordTrieTree muted_word ){
		
		//		// app mute
		//		if( application != null ){
		//			String name = application.name;
		//			if( name != null ){
		//				if( muted_app.contains( name ) ){
		//					return true;
		//				}
		//			}
		//		}
		//
		// word mute
		if( decoded_content != null && muted_word.containsWord( decoded_content.toString() ) ){
			return true;
		}
		
		if( decoded_spoiler_text != null && muted_word.containsWord( decoded_spoiler_text.toString() ) ){
			return true;
		}
		
		//		// reblog
		//		return reblog != null && reblog.checkMuted( muted_app, muted_word );
		
		return false;
		
	}
	
	public boolean hasMedia(){
		return media_attachments != null && media_attachments.size() > 0;
	}
	
	@Override public boolean canPin( SavedAccount access_info ){
		return false;
	}
}
