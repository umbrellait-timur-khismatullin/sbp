import 'dart:convert';
import 'dart:typed_data';

/// данные, которые приходят из натива Android
class ApplicationInfoModel {
  /// [name] - имя приложения
  late final String name;

  /// [packageName] - packageName приложения
  late final String packageName;

  /// [packageName] - иконка приложения
  late final Uint8List? bitmap;

  /// id банка
  late String schema;

  /// Получение данных из словаря
  ApplicationInfoModel.fromJson(Map<dynamic, dynamic> json) {
    final list = List<int>.from(json['bitmap'] ?? []);
    name = json['appName'] ?? '';
    packageName = json['packageName'] ?? '';
    bitmap = Uint8List.fromList(list); //json['bitmap'] ?? '';
    schema = json['schema'];
  }

  /// Получение package_name в виде словаря
  Map<String, dynamic> toMapPackageName() =>
      {
        'package_name': packageName,
      };
}
