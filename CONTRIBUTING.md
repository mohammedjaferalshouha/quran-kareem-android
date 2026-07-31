# المساهمة | Contributing

## العربية

شكرًا لمساهمتك.

قبل إرسال تعديل:

1. افتح بلاغًا يصف المشكلة أو الميزة المقترحة.
2. اختبر السلوك على أكثر من سورة وصفحة، ولا تعتمد على مثال سورة واحدة.
3. لأي خطأ في النص أو الترقيم، أرفق السورة والآية ورقم الصفحة المرجعي.
4. لا تضف نصًا قرآنيًا أو ترجمة أو تفسيرًا أو صوتًا دون ذكر المصدر والترخيص.
5. لا ترفع مفاتيح توقيع أو كلمات مرور أو بيانات حساب.
6. شغّل الاختبارات وابنِ نسخة الاختبار قبل إرسال طلب الدمج.

أمر الاختبار والبناء:

```powershell
.\gradlew.bat testMadaniDebugUnitTest --tests com.quran.labs.androidquran.data.QuranDisplayDataSearchTest
.\gradlew.bat assembleMadaniDebug
```

جميع المساهمات في كود المشروع يجب أن تكون متوافقة مع:

`GNU GPL-3.0`

## English

Thank you for contributing.

Before submitting a change:

1. Open an issue describing the bug or proposed feature.
2. Test across multiple surahs and pages, not only one example.
3. For Quran text or pagination issues, include the surah, ayah, and reference page number.
4. Do not add Quran text, translations, tafsir, fonts, or audio without source and license information.
5. Never commit signing keys, passwords, or account data.
6. Run tests and build the debug variant before opening a pull request.

```powershell
.\gradlew.bat testMadaniDebugUnitTest --tests com.quran.labs.androidquran.data.QuranDisplayDataSearchTest
.\gradlew.bat assembleMadaniDebug
```

Code contributions must be compatible with:

`GNU GPL-3.0`
