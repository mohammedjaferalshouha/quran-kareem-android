# المصادر والتراخيص | Sources and licensing

## العربية

هذا المستند يوضح أصل الكود والبيانات والخدمات التي يعتمد عليها التطبيق. يجب الرجوع إلى شروط كل مزود قبل إعادة توزيع بياناته أو استخدامها تجاريًا؛ فرخصة كود التطبيق لا تمنح تلقائيًا ترخيصًا لجميع النصوص أو الخطوط أو الترجمات أو التفاسير أو الملفات الصوتية.

### الكود الأساسي

- المشروع الأساسي: [Quran for Android](https://github.com/quran/quran_android)
- نقطة الأساس المستخدمة عند بدء التعديلات:

`2dc59d24ea66c87bd8f492d2b0d79d2e8aa04995`

- رخصة الكود: [GNU General Public License v3.0](LICENSE)
- التعديلات الخاصة بهذا المستودع منشورة تحت الرخصة نفسها.
- يجب إبقاء إشعارات الحقوق ونشر الكود المصدري المقابل عند توزيع نسخة تنفيذية، وفق شروط الرخصة.

### النص القرآني والتفاسير والترجمات

- [مشروع المصحف الإلكتروني بجامعة الملك سعود](https://quran.ksu.edu.sa): مصدر لعدد من التفاسير العربية والترجمات، ولمادة تلاوة الشيخ أيمن سويد وفق بيانات المشروع الأساسي.
- [الباحث القرآني](https://tafsir.app): النص العثماني وتفاسير عربية وفق بيانات المشروع الأساسي.
- [موسوعة القرآن الكريم](https://quranenc.com): ترجمات لعدد من اللغات.
- [Tanzil](https://tanzil.net): بعض الترجمات.
- [Noorhidayat](http://www.noorehidayat.org): خط نورحِرا وترجمة مفتي تقي وفق بيانات المشروع الأساسي.

### الصفحات والخطوط

- [quran.com images](https://github.com/quran/quran.com-images): مشروع توليد صفحات المصحف المبني على خطوط مجمع الملك فهد لطباعة المصحف الشريف.
- يستخدم هذا المستودع بنية مزود صفحات المدينة وبيانات مواضع الآيات الموروثة من المشروع الأساسي حتى عند عرض النص المرن.

### التلاوات

- [QuranicAudio](https://quranicaudio.com): استضافة تلاوات صوتية.
- [EveryAyah mirrors](https://everyayah.com): روابط صوتية على مستوى الآيات لبعض القراء، كما هي معرفة في مزود صفحات المدينة.
- قد تتغير روابط الاستضافة وشروطها؛ راجع الجهة المالكة قبل إعادة استضافة الملفات الصوتية.

### مكتبات مفتوحة المصدر

من أبرز المكتبات المستخدمة:

- [AndroidX](https://developer.android.com/jetpack/androidx)
- [Kotlin](https://kotlinlang.org)
- [Material Components for Android](https://github.com/material-components/material-components-android)
- [OkHttp](https://github.com/square/okhttp)
- [Moshi](https://github.com/square/moshi)
- [RxJava](https://github.com/ReactiveX/RxJava)
- [RxAndroid](https://github.com/ReactiveX/RxAndroid)
- [Timber](https://github.com/JakeWharton/timber)
- [NumberPicker](https://github.com/ShawnLin013/NumberPicker)

القائمة الدقيقة للإصدارات والاعتماديات موجودة في ملفات بناء المشروع.

### ما لم يُستخدم مباشرة

تمت دراسة مستودعات بيانات أخرى أثناء التخطيط، لكن لا ينبغي اعتبارها مصدرًا لهذا الإصدار ما لم تظهر ملفاتها أو إشعاراتها داخل شجرة المصدر. التوثيق هنا مبني على الاعتماديات والإشعارات الموجودة فعليًا في المستودع.

## English

This document records the origin of the code, data, and services used by the application. Check each provider's current terms before redistributing or commercially using its data. The application code license does not automatically license every text, font, translation, tafsir, or audio file.

### Upstream code

- Upstream project: [Quran for Android](https://github.com/quran/quran_android)
- Upstream base commit:

`2dc59d24ea66c87bd8f492d2b0d79d2e8aa04995`

- Code license: [GNU General Public License v3.0](LICENSE)
- Modifications in this repository are distributed under the same license.
- Distributors must preserve notices and provide the corresponding source as required by the license.

### Quran text, tafsir, and translations

- [King Saud University Electronic Moshaf](https://quran.ksu.edu.sa): a source of Arabic tafsir, translations, and Dr. Ayman Suwaid recitation data according to upstream notices.
- [Al-Bahith Al-Qurani](https://tafsir.app): Uthmani text and Arabic tafsir according to upstream notices.
- [QuranEnc](https://quranenc.com): translations in multiple languages.
- [Tanzil](https://tanzil.net): selected translations.
- [Noorhidayat](http://www.noorehidayat.org): Noorehira font and Mufti Taqi translation according to upstream notices.

### Page and font data

- [quran.com images](https://github.com/quran/quran.com-images): mushaf page generation based on King Fahd Quran Printing Complex fonts.
- This repository retains upstream Madani page-provider and ayah-position data for flexible text pagination.

### Recitations

- [QuranicAudio](https://quranicaudio.com): recitation hosting.
- [EveryAyah](https://everyayah.com): ayah-level audio mirrors referenced by the Madani page provider.
- Hosting links and terms may change. Review the rights holder's terms before rehosting audio.

### Open-source libraries

Major dependencies include AndroidX, Kotlin, Material Components, OkHttp, Moshi, RxJava, RxAndroid, Timber, and NumberPicker. Exact versions are declared in the Gradle version catalog and build files.

### Repositories evaluated but not directly bundled

Other data repositories were evaluated during planning. They are not listed as sources of this release unless their files or notices are present in this source tree.
