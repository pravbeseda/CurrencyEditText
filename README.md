# CurrencyEditText

A library to dynamically format your `EditTexts` to take currency inputs.

This project is fork of https://github.com/CottaCush/CurrencyEditText
Also there was used ideas of another project: https://github.com/firmfreez/CurrencyEditText
Thanks a lot to both authors.

Attention! This is an alpha release of the library, it is under active development.

[![ci](https://github.com/pravbeseda/CurrencyEditText/actions/workflows/ci.yml/badge.svg)](https://github.com/pravbeseda/CurrencyEditText/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/ru.pravbeseda/CurrencyEditText.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22ru.pravbeseda%22%20AND%20a:%22CurrencyEditText%22)

## Gradle Dependency

Add the dependency to your app's `build.gradle`:

```groovy
implementation 'ru.pravbeseda:CurrencyEditText:<insert-latest-version-here>'
```

For versions, kindly head over to
the [releases page](https://github.com/pravbeseda/CurrencyEditText/releases)

## Library content

There are 2 components in this library `CurrencyEditText` and `CurrencyMaterialEditText`.
The `CurrencyMaterialEditText` component is inherited from `TextInputLayout` and is a wrapper
over `CurrencyEditText`.

## Usage

You can add the `CurrencyEditText` to your layout.

```xml
<ru.pravbeseda.currencyedittext.CurrencyEditText 
    android:layout_width="wrap_content"
    android:layout_height="60dp" 
    android:ems="10" 
    android:id="@+id/editText"
    android:text="1234.67"
    app:negativeValueAllow="true"
    app:maxNumberOfDecimalPlaces="2"
/>
```

Or you can use the `CurrencyMaterialEditText` component.

```xml
<ru.pravbeseda.currencyedittext.CurrencyMaterialEditText 
    android:layout_width="wrap_content"
    android:layout_height="60dp" 
    android:ems="10" 
    android:id="@+id/editText"
    app:text="1234.67"
    app:negativeValueAllow="true"
    app:maxNumberOfDecimalPlaces="2"
/>
```

That's all for basic setup. Your `editText` should automatically format currency inputs.

After that, you can configure the View parameters separately in the code:

```Kotlin
edittext.setNegativeValueAllow(true)
edittext.setMaxNumberOfDecimalPlaces(2)
```

You can set the value via `setValue` method:

```Kotlin
edittext.setValue(BigDecimal(4321.76))
```

Or you can set the text value directly:

```Kotlin
edittext.setText("4321.76")
```

But in the last case the value is needed to be formatted according to CurrencyEditText localization settings.

## Localization

The library supports localization. You can set the locale in the layout file:

```
app:localeTag="da-DK"
```

Or in the code:

```Kotlin
edittext.setLocale(Locale("da", "DK"))
// or
edittext.setLocale(Locale.GERMAN)
// or
edittext.setLocale("da-DK")
```

You can just set the decimal and grouping separators:

```
app:decimalSeparator=","
app:groupingSeparator=" "
```

Or in the code:

```Kotlin
edittext.setDecimalSeparator(",")
edittext.setGroupingSeparator(" ")
```

## CurrencyMaterialEditText Features

Since CurrencyMaterialEditText is not a descendant of EditText, some EditText properties are passed with the prefix 'app'. For example:

```
app:text="1234.67"
app:selectAllOnFocus="true"
```

## License

    Copyright (c) 2022-2023 Alexander Ivanov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
