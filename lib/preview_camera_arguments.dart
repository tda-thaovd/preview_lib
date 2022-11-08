import 'package:flutter/material.dart';

/// Camera args for [CameraView].
class PreviewCameraArguments {
  /// The texture id.
  final int? textureId;

  /// Size of the texture.
  final Size size;

  /// Create a [PreviewCameraArguments].
  PreviewCameraArguments({
    this.textureId,
    required this.size,
  });
}
