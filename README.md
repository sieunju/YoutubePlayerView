> 안녕하세요. WebView 방식의 유튜브 플레이어 라이브러리 입니다.

---

1. #### 컨셉
    - 외부 라이브러리를 사용하지 않고 최소한으로 구현
2. #### 사양
    - Min SDK Version 21
    - Max SDK Version 30

3. #### 라이브러리 추가 하는 방법
    - *Project Gradle*
    ~~~
    allprojects {
	    repositories {
		    ...
		    maven { url 'https://jitpack.io' }
	    }
    }
    ~~~
    - *App Module Gradle*
    ~~~
    dependencies {
        implementation 'com.github.sieunju:YoutubePlayerView:0.0.1'
    }
    ~~~

4. #### 사용 예
    - *xml*
    ~~~
    <com.hmju.youtubeplayer.YoutubePlayerView
        android:id="@+id/youtubeView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:youtube_thumbnail_id="@id/imgThumb">

        <androidx.appcompat.widget.AppCompatImageView
            android:id="@+id/imgThumb"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:adjustViewBounds="true"
            android:scaleType="centerCrop" />
    </com.hmju.youtubeplayer.YoutubePlayerView>
    ~~~
    - *attrs*
    ~~~
        <declare-styleable name="YoutubePlayerView">
        <attr name="youtube_thumbnail_id" format="reference"/> <!-- 썸네일 아이디값 -->
        <attr name="youtube_is_auto_play" format="boolean"/> <!-- 자동 재생 유무 -->
        <attr name="youtube_is_web_control" format="boolean"/> <!-- 웹뷰에서 제공하는 컨트롤러 의존 유무 -->
        <attr name="youtube_is_logo" format="boolean"/> <!-- 로고 표시 유무 -->
        <attr name="youtube_is_loop" format="boolean"/> <!-- 연속 재생 유무 -->
        <attr name="youtube_is_effect" format="boolean"/> <!-- 동영상 특수효과 유무 -->
        <attr name="youtube_is_relation" format="boolean"/> <!-- 관련 영상 표시 유무 -->
        <attr name="youtube_load_policy" format="integer"/> <!-- 1로 설정시 사용자가 자막을 중지하더라도 표시 -->
        <attr name="youtube_lang_pref" format="string"/> <!-- 자막 표시 언어 코드 -->
        <attr name="youtube_is_fullscreen" format="boolean"/> <!-- 전체 화면 표시 유무 -->
        <attr name="youtube_is_share" format="boolean"/> <!-- 공유하기 버튼 유무 -->
    </declare-styleable>
    ~~~

*ps.안정화 완료 되면 1.0.0 올릴 예정. attribute 삭제될 파라미터가 있습니다.*
