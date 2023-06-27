import Flutter
import UIKit

public class SwiftSbpPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "sbp", binaryMessenger: registrar.messenger())
        let instance = SwiftSbpPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "getInstalledBanks" {
            let arguments = (call.arguments as? Dictionary<String,Any>)!
            let schemaApplications = (arguments["schema_applications"] as? Array<String>)!
            
            getBanks(schemaApplications,result:result)
        } else if call.method == "openBank" {
            let arguments = (call.arguments as? Dictionary<String,String>)!
            let url = schemaLink(arguments["schema"]!,arguments["url"]!)
            
            open(url, result:result)
        }
        else{
            result(FlutterMethodNotImplemented)
        }
    }
    public func getBanks(_ schemaApplications: [String], result: @escaping FlutterResult) {
        var schemas: [String] = []
        for schemaApplication in schemaApplications{
            schemas.append(schemaApplication)
        }
        result(schemas)
    }
    
    
    public func open(_ url: URL, result: @escaping FlutterResult) {
        UIApplication.shared.open(url, options: [:]) {
            success in if(!success) {
                result(false)
            } else {
                result(true)
            }
        }
    }
    
    public func schemaLink(_ scheme: String,_ link: String)->URL{
        let url = URL(string: link.replacingOccurrences(of: "https://", with: "\(scheme)://").replacingOccurrences(of: "http://", with: "\(scheme)://"))!
        return url
    }
}
