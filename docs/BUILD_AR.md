<div dir="rtl" align="right">

# البناء والتوقيع

## المتطلبات

- نظام يدعم تطوير أندرويد.
- حزمة تطوير أندرويد مثبتة.
- أندرويد ستوديو أو سطر الأوامر.
- حزمة تطوير جافا رقم ٢١.

## نسخة الاختبار

من جذر المشروع:

```powershell
.\gradlew.bat testMadaniDebugUnitTest --tests com.quran.labs.androidquran.data.QuranDisplayDataSearchTest
.\gradlew.bat assembleMadaniDebug
```

ملف البناء الناتج:

```text
app\build\outputs\apk\madani\debug\app-madani-debug.apk
```

## نسخة الإصدار

أنشئ مفتاح توقيع خاصًا بك واحفظه خارج المستودع. لا ترفع المفتاح أو كلمة مروره إلى أي مستودع عام.

```powershell
.\gradlew.bat assembleMadaniRelease -PSTORE_FILE="PATH_TO_KEYSTORE" -PSTORE_PASSWORD="STORE_PASSWORD" -PKEY_ALIAS="KEY_ALIAS" -PKEY_PASSWORD="KEY_PASSWORD"
```

ملف البناء الناتج:

```text
app\build\outputs\apk\madani\release\app-madani-release.apk
```

## تحديث التطبيق لاحقًا

- احتفظ بمفتاح التوقيع نفسه لجميع الإصدارات.
- ارفع رقم الإصدار ورقم البناء.
- ابنِ الإصدار بالمفتاح نفسه.
- يمكن للمستخدم تثبيت الإصدار الأحدث فوق القديم مع الاحتفاظ بالبيانات إذا بقي معرّف التطبيق والتوقيع كما هما.

</div>
