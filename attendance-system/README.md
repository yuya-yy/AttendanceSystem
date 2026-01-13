# 勤怠管理システム

## 概要

ブラウザから操作できる勤怠管理システムです。
一般ユーザーは出勤/退勤の打刻や勤務場所・連絡先の登録ができます。
管理者はユーザー管理や勤怠実績の確認ができます。

## 主な機能

一般ユーザー

- ログイン／ログアウト
- 出勤（打刻）
- 退勤（打刻）
- 勤務場所の登録／変更
- 連絡先（メール／電話）の登録／変更
- 部署内の勤怠状況一覧表示
- ユーザー一覧表示

管理者

- 新規ユーザー登録
- ユーザー編集
- ユーザー削除（論理削除）
- 勤怠実績一覧（集計表示）

## 画面一覧（S0101〜S0109）

- S0101：ログイン画面
- S0102：勤怠入力画面（出勤／退勤、直近履歴表示）
- S0103：勤務場所登録画面
- S0104：連絡先登録画面
- S0105：勤怠状況一覧（同じ部署の現在状況）
- S0106：ユーザー情報一覧（管理者は操作ボタン表示）
- S0107：勤怠実績一覧（管理者：日別＋月合計）
- S0108：ユーザー編集（管理者）
- S0109：新規ユーザー登録（管理者）

## 使用技術

- Java
- Spring Boot
- Thymeleaf
- Spring Data JPA（Hibernate）
- PostgreSQL
- Maven（mvnw）
- Lombok

## 動作確認環境

- OS: Windows 11
- Java: 21
- PostgreSQL: 17.4
- Spring Boot: 4.0.1

## URL 一覧

認証

- GET `/auth/login` ：ログイン画面表示
- POST `/auth/login` ：ログイン実行
- POST `/auth/logout` ：ログアウト

勤怠

- GET `/attendance` ：勤怠入力画面
- POST `/attendance/clock-in` ：出勤
- POST `/attendance/clock-out` ：退勤
- GET `/attendance/work-location` ：勤務場所登録画面
- POST `/attendance/work-location` ：勤務場所更新
- GET `/attendance/contact` ：連絡先登録画面
- POST `/attendance/contact` ：連絡先更新
- GET `/attendance/status` ：勤怠状況一覧（同じ部署のみ）

ユーザー管理

- GET `/users/list` ：ユーザー情報一覧
- GET `/users/new` ：新規ユーザー登録画面（管理者）
- POST `/users/new` ：新規ユーザー登録（管理者）
- GET `/users/{userId}/edit` ：ユーザー編集画面（管理者）
- POST `/users/{userId}/update` ：ユーザー更新（管理者）
- POST `/users/{userId}/delete` ：ユーザー削除（論理削除・管理者）
- GET `/reports/{userId}` ：勤怠実績一覧（日別＋月合計）

## セットアップ手順

### 1. PostgreSQL に DB を作成

- DB 名: attendance_system

### 2. テーブル作成・テスト用初期データ投入

`attendance-system/db/` 配下のSQLを実行してください

- schema.sql （テーブル作成用sql）
- 004_department_limit_trigger.sql（部署数制限用sql）

- 001_master_init.sql（勤務場所、部署作成用サンプルsql）
- 002_sample_users.sql（ユーザー400人サンプルsql）
- 003_sample_attendance_records.sql（勤怠記録作成用サンプルsql）

### 3. アプリ設定

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/attendance_system
spring.datasource.username=TODO_DB_USER
spring.datasource.password=TODO_DB_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false

spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Tokyo
spring.datasource.hikari.connection-init-sql=SET TIME ZONE 'Asia/Tokyo';

spring.messages.basename=messages
spring.messages.encoding=UTF-8
```

### 4. 起動方法

```bash
./mvnw spring-boot:run
```

- ブラウザでアクセス

`http://localhost:8080/auth/login`

### ログイン情報（管理者）

- ユーザー名: `user001`
- パスワード: `password`
- 権限: `role = 1`

## テスト実行方法

```bash
./mvnw test
```

## DB

テーブル

- users：ユーザー（論理削除 `deleted_at`）
- attendance_records：勤怠記録（出勤・退勤）
- departments：部署
- work_locations：勤務場所マスタ

権限（role）

- role = 1：管理者
- role = 2：一般ユーザー

論理削除

- `deleted_at IS NULL` を有効データとして扱う
- 削除されたユーザーは一覧に出ず、ログインも不可

## 開発ルール（Git）

- master：完成版
- develop：featureブランチをマージ
- feature/0001 作業ブランチ

コミットメッセージ例

- `#0033 タイムゾーンをAsia/Tokyoに統一`
- `#0034 ユーザー一覧のフィルタ機能を改善`

## サンプルデータの方針

- 部署: 10部署
- ユーザー: 400人（部署最大50人）
- 勤務場所: 5場所
- 勤怠: 直近90日分
- 電話番号:
