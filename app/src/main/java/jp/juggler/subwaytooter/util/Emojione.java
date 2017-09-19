package jp.juggler.subwaytooter.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.juggler.subwaytooter.App1;
import jp.juggler.subwaytooter.R;
import jp.juggler.subwaytooter.api.entity.CustomEmojiMap;

public abstract class Emojione {
	private static final Pattern SHORTNAME_PATTERN = Pattern.compile( ":([-+\\w]+):" );
	
	public static final HashMap< String, String > map_name2unicode = EmojiMap._shortNameToUnicode;
	private static final HashSet< String > set_unicode = EmojiMap._unicode_set;
	
	private static class DecodeEnv {
		SpannableStringBuilder sb = new SpannableStringBuilder();
		int last_span_start = - 1;
		int last_span_end = - 1;
		
		@Nullable CustomEmojiMap custom_map;
		
		DecodeEnv( @Nullable CustomEmojiMap custom_map ){
			this.custom_map = custom_map;
		}
		
		void closeSpan(){
			if( last_span_start >= 0 ){
				if( last_span_end > last_span_start ){
					EmojiSpan typefaceSpan = new EmojiSpan( App1.typeface_emoji );
					sb.setSpan( typefaceSpan, last_span_start, last_span_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				}
				last_span_start = - 1;
			}
		}
		
		void addEmoji( String s ){
			if( last_span_start < 0 ){
				last_span_start = sb.length();
			}
			sb.append( s );
			last_span_end = sb.length();
		}
		
		void addUnicodeString( String s ){
			int i = 0;
			int end = s.length();
			while( i < end ){
				int remain = end - i;
				String emoji = null;
				for( int j = EmojiMap.max_length ; j > 0 ; -- j ){
					if( j > remain ) continue;
					String check = s.substring( i, i + j );
					if( ! set_unicode.contains( check ) ) continue;
					emoji = check;
					break;
				}
				if( emoji != null ){
					addEmoji( emoji );
					i += emoji.length();
					continue;
				}
				closeSpan();
				int length = Character.charCount( s.codePointAt( i ) );
				if( length == 1 ){
					sb.append( s.charAt( i ) );
					++ i;
				}else{
					sb.append( s.substring( i, i + length ) );
					i += length;
				}
			}
		}
		
		void addImageSpan( String text, Context context, int res_id ){
			closeSpan();
			int start = sb.length();
			sb.append( text );
			int end = sb.length();
			sb.setSpan( new EmojiImageSpan( context, res_id ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		
		void addNetworkEmojiSpan( String text, @NonNull String url ){
			closeSpan();
			int start = sb.length();
			sb.append( text );
			int end = sb.length();
			sb.setSpan( new NetworkEmojiSpan( url ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
	}
	
	private static final Pattern reNicoru = Pattern.compile( "\\Anicoru\\d*\\z", Pattern.CASE_INSENSITIVE );
	private static final Pattern reHohoemi = Pattern.compile( "\\Ahohoemi\\d*\\z", Pattern.CASE_INSENSITIVE );
	
	public static Spannable decodeEmoji( Context context, String s, @Nullable CustomEmojiMap custom_map ){
		
		DecodeEnv decode_env = new DecodeEnv( custom_map );
		Matcher matcher = SHORTNAME_PATTERN.matcher( s );
		int last_end = 0;
		while( matcher.find() ){
			int start = matcher.start();
			int end = matcher.end();
			if( start > last_end ){
				decode_env.addUnicodeString( s.substring( last_end, start ) );
			}
			last_end = end;
			//
			String unicode = map_name2unicode.get( matcher.group( 1 ) );
			if( unicode == null ){
				String url = ( custom_map == null ? null : custom_map.get( matcher.group( 1 ) ) );
				if( ! TextUtils.isEmpty( url ) ){
					decode_env.addNetworkEmojiSpan( s.substring( start, end ), url );
					
				}else if( reHohoemi.matcher( matcher.group( 1 ) ).find() ){
					decode_env.addImageSpan( s.substring( start, end ), context, R.drawable.emoji_hohoemi );
				}else if( reNicoru.matcher( matcher.group( 1 ) ).find() ){
					decode_env.addImageSpan( s.substring( start, end ), context, R.drawable.emoji_nicoru );
				}else{
					decode_env.addUnicodeString( s.substring( start, end ) );
				}
			}else{
				decode_env.addEmoji( unicode );
			}
		}
		// copy remain
		int end = s.length();
		if( end > last_end ){
			decode_env.addUnicodeString( s.substring( last_end, end ) );
		}
		// close span
		decode_env.closeSpan();
		
		return decode_env.sb;
	}
}
