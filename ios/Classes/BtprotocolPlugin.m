#import "BtprotocolPlugin.h"
#if __has_include(<btprotocol/btprotocol-Swift.h>)
#import <btprotocol/btprotocol-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "btprotocol-Swift.h"
#endif

@implementation BtprotocolPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBtprotocolPlugin registerWithRegistrar:registrar];
}
@end
