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
        implementation 'com.github.sieunju:YoutubePlayerView:0.0.3'
    }
    ~~~

4. #### 기능
    - 유튜브 재생, 일시중지, 중지 기능
    - 공유하기 및 Youtube 앱으로 재생하기
    - 전체 화면
    - 현재 진행 상황 표시 및 원하는 시간으로 이동
    
5. #### 사용 예
    - *xml(기본)*
    ~~~
    <com.hmju.youtubeplayer.YoutubePlayerView
        android:id="@+id/youtubeView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>
    ~~~
    
    - *xml(외부 이미지 라이브러리로 썸네일 표현하는 경우)*
    ~~~
    <com.hmju.youtubeplayer.YoutubePlayerView
        android:id="@+id/youtubeView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:youtubeThumbnailId="@id/imgThumb">

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
        <attr name="youtubeThumbnailId" format="reference"/> <!-- 썸네일 아이디값 -->
        <attr name="youtubeIsAutoPlay" format="boolean"/> <!-- 자동 재생 유무 -->
        <attr name="youtubeIsWebControl" format="boolean"/> <!-- 웹뷰에서 제공하는 컨트롤러 의존 유무 -->
        <attr name="youtubeIsLogo" format="boolean"/> <!-- 로고 표시 유무 -->
        <attr name="youtubeIsLoop" format="boolean"/> <!-- 연속 재생 유무 -->
        <attr name="youtubeIsEffect" format="boolean"/> <!-- 동영상 특수효과 유무 -->
        <attr name="youtubeIsRelation" format="boolean"/> <!-- 관련 영상 표시 유무 -->
        <attr name="youtubeLoadPolicy" format="integer"/> <!-- 1로 설정시 사용자가 자막을 중지하더라도 표시 -->
        <attr name="youtubeLangPref" format="string"/> <!-- 자막 표시 언어 코드 -->
        <attr name="youtubeIsFullscreen" format="boolean"/> <!-- 전체 화면 표시 유무 -->
        <attr name="youtubeIsShare" format="boolean"/> <!-- 공유하기 버튼 유무 -->
        <attr name="youtubeSkipThumbNail" format="boolean"/> <!-- 내/외부 썸네일 처리 스킵 유무 -->
    </declare-styleable>
    ~~~
