import 'package:flutter/material.dart';
import 'package:preview_camera_lib/preview_camera_arguments.dart';
import 'package:preview_camera_lib/preview_camera_controller.dart';

class PreviewCamera extends StatefulWidget {
  const PreviewCamera({Key? key, this.controller}) : super(key: key);

  /// The controller of the camera.
  final PreviewCameraController? controller;

  @override
  State<PreviewCamera> createState() => _PreviewCameraState();
}

class _PreviewCameraState extends State<PreviewCamera>
    with WidgetsBindingObserver {
  late PreviewCameraController controller;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    controller = widget.controller ?? PreviewCameraController();
    controller.start();
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder(
      valueListenable: controller.startArguments,
      builder: (context, value, child) {
        value = value as PreviewCameraArguments?;
        if (value == null) {
          return const ColoredBox(color: Colors.black);
        } else {
          return ClipRect(
            child: SizedBox(
              width: MediaQuery.of(context).size.width,
              height: MediaQuery.of(context).size.height,
              child: FittedBox(
                child: SizedBox(
                  width: value.size.width,
                  height: value.size.height,
                  child: Texture(textureId: value.textureId!),
                ),
              ),
            ),
          );
        }
      },
    );
  }
}
