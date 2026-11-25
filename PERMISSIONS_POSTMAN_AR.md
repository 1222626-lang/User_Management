# إدارة الصلاحيات والأدوار من Postman

جميع مسارات `/api/authorization/**` تتطلب رمز دخول لمستخدم يملك الصلاحية `AUTHORIZATION_MANAGE` (عادةً حساب المدير).

## تسجيل الدخول والحصول على الرمز

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "كلمة_مرور_المدير"
}
```

انسخ `accessToken` من الاستجابة، ثم في Postman اختر **Authorization → Bearer Token** والصق الرمز.

## إدارة الأدوار

### عرض جميع الأدوار

```http
GET http://localhost:8080/api/authorization/roles
```

### إنشاء دور مخصص

```http
POST http://localhost:8080/api/authorization/roles
Content-Type: application/json
```

```json
{
  "name": "SUPERVISOR",
  "description": "مشرفو النظام"
}
```

تعود الاستجابة `201 Created` ومعها معرّف الدور. الاسم فريد، بحد أقصى 50 حرفًا، يبدأ بحرف ويحتوي أحرفًا أو أرقامًا أو `_` فقط. لا يلزم تغيير الكود لإنشاء دور جديد.

### تعديل اسم الدور أو وصفه

```http
PUT http://localhost:8080/api/authorization/roles/3
Content-Type: application/json
```

```json
{
  "name": "SUPERVISOR",
  "description": "مشرفو النظام وإدارة التقارير"
}
```

استبدل `3` بمعرّف الدور. يمكن إدارة أذونات الدور من المسار الموضح لاحقًا. لا يمكن تغيير أسماء دوري النظام `ADMIN` و`USER`.

### حذف دور مخصص

```http
DELETE http://localhost:8080/api/authorization/roles/3
```

يرجع الطلب `204 No Content` عند نجاح الحذف. لا يسمح بحذف `ADMIN` أو `USER`، ولا بحذف دور ما زال مسندًا إلى مستخدم. أعد إسناد مستخدميه إلى دور آخر أولًا.

## إدارة الصلاحيات

### عرض الصلاحيات

```http
GET http://localhost:8080/api/authorization/permissions
```

### إنشاء صلاحية

```http
POST http://localhost:8080/api/authorization/permissions
Content-Type: application/json
```

```json
{
  "name": "REPORT_EXPORT",
  "description": "السماح بتصدير التقارير"
}
```

تُحفظ الصلاحية في قاعدة البيانات وتعود الاستجابة `201 Created`. الاسم فريد، بحد أقصى 100 حرف، يبدأ بحرف ويحتوي أحرفًا أو أرقامًا أو `_` فقط. الوصف اختياري وبحد أقصى 255 حرفًا.

### تعديل صلاحية

```http
PUT http://localhost:8080/api/authorization/permissions/15
Content-Type: application/json
```

```json
{
  "name": "REPORT_EXPORT",
  "description": "السماح بتصدير التقارير"
}
```

استبدل `15` بمعرّف الصلاحية. لا يمكن استخدام اسم موجود لصلاحية أخرى.

### حذف صلاحية

```http
DELETE http://localhost:8080/api/authorization/permissions/15
```

يحذف السجل وارتباطاته بالأدوار وتجاوزات المستخدمين، ويرجع `204 No Content`.

## ربط الصلاحيات بدور

بعد إنشاء الصلاحيات، استخدم معرّف الدور من `GET /api/authorization/roles` وأرسل قائمة الصلاحيات الكاملة التي تريد أن يحتفظ بها الدور. الطلب يستبدل قائمة الدور الحالية:

```http
PUT http://localhost:8080/api/authorization/roles/3/permissions
Content-Type: application/json
```

```json
{
  "permissionNames": [
    "USER_PROFILE_READ",
    "USER_PROFILE_UPDATE",
    "REPORT_EXPORT"
  ]
}
```

## ملاحظة مهمة حول حماية المسارات

إنشاء صلاحية وربطها بدور يخزنها ويتيحها ضمن صلاحيات المستخدم الفعلية، لكن النظام لا يستنتج تلقائيًا أي مسار API جديد يجب أن تحميه. تعيين صلاحية لمسار جديد ما زال يتطلب تعريف قاعدة حماية ذلك المسار في إعدادات الأمن.

## التشغيل والاختبارات

يتطلب المشروع Java 21 وMySQL وفق إعدادات `src/main/resources/application.properties`. شغّل التطبيق من المجلد الذي يحتوي `pom.xml`:

```bash
./mvnw spring-boot:run
```

على Windows:

```bat
mvnw.cmd spring-boot:run
```

تشغيل الاختبارات:

```bash
./mvnw clean test
```
