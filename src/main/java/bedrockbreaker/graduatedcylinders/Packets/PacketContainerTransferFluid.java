package bedrockbreaker.graduatedcylinders.Packets;

import bedrockbreaker.graduatedcylinders.FluidHelper;
import bedrockbreaker.graduatedcylinders.Proxy.ProxyFluidHandlerItem;
import bedrockbreaker.graduatedcylinders.Proxy.ProxyFluidStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class PacketContainerTransferFluid implements IMessage {

	private int slot;
	private boolean valid;

	public PacketContainerTransferFluid() {
		this.valid = false;
	}

	public PacketContainerTransferFluid(int slot) {
		this.slot = slot;
		this.valid = true;
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		try {
			this.slot = buffer.readInt();
		} catch(IndexOutOfBoundsException error) {
			System.out.println(error);
		}
		this.valid = true;
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		if (!this.valid) return;
		buffer.writeInt(this.slot);
	}

	public static class Handler implements IMessageHandler<PacketContainerTransferFluid, IMessage> {
		
		@Override
		public IMessage onMessage(PacketContainerTransferFluid message, MessageContext ctx) {
			if (!message.valid || ctx.side != Side.SERVER) return null;
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
				EntityPlayer player = ctx.getServerHandler().player;
				Container container = player.openContainer;
				Slot hoveredSlot = container.inventorySlots.get(message.slot);

				ProxyFluidHandlerItem heldFluidHandler = FluidHelper.getProxyFluidHandler(player.inventory.getItemStack());
				ProxyFluidHandlerItem underFluidHandler = FluidHelper.getProxyFluidHandler(hoveredSlot.getStack());

				int transferAmount = FluidHelper.getTransferAmount(heldFluidHandler, underFluidHandler);
				if (transferAmount == 0) return;

				ProxyFluidStack fluidStack = heldFluidHandler.getTankProperties().get(0).getContents();
				if (fluidStack == null) fluidStack = underFluidHandler.getTankProperties().get(0).getContents();
				if (fluidStack == null) return;
				fluidStack = new ProxyFluidStack(fluidStack, Math.abs(transferAmount));

				if (FluidHelper.tryFluidTransfer(transferAmount < 0 ? underFluidHandler : heldFluidHandler, transferAmount < 0 ? heldFluidHandler : underFluidHandler, fluidStack, true) != null) player.world.playSound(null, player.getPosition(), transferAmount < 0 ? fluidStack.getEmptySound() : fluidStack.getFillSound(), SoundCategory.PLAYERS, 1.0F, 1.0F);
				
				// What the heck? Why do I have to switch these? I thought I cancelled the right-click event in InventoryHandler...
				hoveredSlot.putStack(heldFluidHandler.getContainer());
				player.inventory.setItemStack(underFluidHandler.getContainer());

				if (!(player instanceof EntityPlayerMP)) return;
				((EntityPlayerMP) player).isChangingQuantityOnly = false;
				((EntityPlayerMP) player).updateHeldItem();
			});
			return null;
		}
	}
	
}
